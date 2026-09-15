package experiment.inferred;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import java.nio.file.*;
import java.util.*;

/** Bounded syntax proof: direct scalar getter -> setter copies in one-argument constructors. */
public final class ExtractCopies {
    record Source(Path path, CompilationUnit unit, ClassOrInterfaceDeclaration type) {}
    static final Map<String, Source> types = new TreeMap<>();
    static final Set<String> SCALARS = Set.of("String", "Integer", "Long", "Boolean", "int", "long", "boolean");
    public static void main(String[] args) throws Exception {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_21);
        try (var files = Files.walk(Path.of(args[0]))) {
            for (Path p : files.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
                var cu = StaticJavaParser.parse(p);
                for (var t : cu.getTypes()) if (t instanceof ClassOrInterfaceDeclaration c) {
                    String fqn = cu.getPackageDeclaration().map(x -> x.getNameAsString()+".").orElse("")+c.getNameAsString();
                    types.put(fqn, new Source(p, cu, c));
                }
            }
        }
        var contracts = new ArrayList<Map<String,Object>>();
        for (var entry : types.entrySet()) {
            Source target = entry.getValue();
            if (!target.type().isAnnotationPresent("Entity")) continue;
            for (var ctor : target.type().getConstructors()) {
                if (ctor.getParameters().size()!=1) continue;
                var parameter = ctor.getParameter(0);
                String sourceName = resolve(target, parameter.getTypeAsString());
                Source source = types.get(sourceName);
                if (source==null || sourceName.equals(entry.getKey())) continue;
                var copies = new TreeMap<String,String>();
                var proofs = new ArrayList<Map<String,Object>>();
                for (var statement : ctor.getBody().getStatements()) {
                    if (!(statement instanceof ExpressionStmt e) || !(e.getExpression() instanceof MethodCallExpr set)) continue;
                    if (set.getScope().isPresent() && !set.getScope().get().isThisExpr()) continue;
                    if (set.getArguments().size()!=1 || !(set.getArgument(0) instanceof MethodCallExpr get)) continue;
                    if (!get.getArguments().isEmpty() || get.getScope().isEmpty() || !get.getScope().get().toString().equals(parameter.getNameAsString())) continue;
                    String from = getterField(source, get.getNameAsString());
                    String to = setterField(target, set.getNameAsString());
                    if (from==null || to==null || !scalar(source,from) || !scalar(target,to)) continue;
                    if (copies.containsKey(from)) throw new IllegalStateException("Ambiguous source copy: "+ctor);
                    copies.put(from,to);
                    proofs.add(Map.of("sourceField",from,"targetField",to,"line",statement.getBegin().get().line,"expression",statement.toString()));
                }
                // Framework identity convention; names of application destination fields are inferred.
                if (!copies.containsKey("aggregateId") || copies.size()<2) continue;
                contracts.add(Map.of("sourceType",sourceName,"targetType",entry.getKey(),"sourceKey","aggregateId",
                    "targetKey",copies.get("aggregateId"),"fields",copies,"proof",proofs,"sourceFile",target.path().toString()));
            }
        }
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(Path.of(args[1]).toFile(), contracts);
        System.out.println("Inferred "+contracts.size()+" direct-copy contracts");
    }
    static String resolve(Source s,String name) {
        if(types.containsKey(name))return name;
        String local=s.unit().getPackageDeclaration().map(x->x.getNameAsString()+".").orElse("")+name;
        if(types.containsKey(local))return local;
        var matches=new ArrayList<String>();
        for(var i:s.unit().getImports()) {
            String n=i.isAsterisk()?i.getNameAsString()+"."+name:i.getNameAsString();
            if(n.endsWith("."+name)&&types.containsKey(n))matches.add(n);
        }
        return matches.size()==1?matches.get(0):"";
    }
    static boolean scalar(Source s,String field) {
        return s.type().getFields().stream().flatMap(f->f.getVariables().stream()).anyMatch(v->v.getNameAsString().equals(field)&&SCALARS.contains(v.getTypeAsString()));
    }
    static String getterField(Source s,String name) {
        var ms=s.type().getMethodsByName(name).stream().filter(m->m.getParameters().isEmpty()).toList();
        if(ms.size()!=1||ms.get(0).getBody().isEmpty())return null;
        var body=ms.get(0).getBody().get();
        if(body.getStatements().size()!=1||!(body.getStatement(0) instanceof ReturnStmt r)||r.getExpression().isEmpty())return null;
        return ownField(s,r.getExpression().get());
    }
    static String setterField(Source s,String name) {
        var ms=s.type().getMethodsByName(name).stream().filter(m->m.getParameters().size()==1).toList();
        if(ms.size()!=1||ms.get(0).getBody().isEmpty())return null;
        var m=ms.get(0);var b=m.getBody().get();
        if(b.getStatements().size()!=1||!(b.getStatement(0) instanceof ExpressionStmt e)||!(e.getExpression() instanceof AssignExpr a))return null;
        if(a.getOperator()!=AssignExpr.Operator.ASSIGN||!a.getValue().toString().equals(m.getParameter(0).getNameAsString()))return null;
        if(a.getTarget().isNameExpr()&&a.getTarget().toString().equals(m.getParameter(0).getNameAsString()))return null;
        return ownField(s,a.getTarget());
    }
    static String ownField(Source s,Expression e) {
        String name=e.isNameExpr()?e.asNameExpr().getNameAsString():e.isFieldAccessExpr()&&e.asFieldAccessExpr().getScope().isThisExpr()?e.asFieldAccessExpr().getNameAsString():null;
        return name!=null&&s.type().getFieldByName(name).isPresent()?name:null;
    }
}
