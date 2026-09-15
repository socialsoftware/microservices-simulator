package experiment.inferred;

import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Experiment-only observer. No Quizzes types, application field names, or expected values. */
public final class Probe {
    static final ObjectMapper JSON=new ObjectMapper().findAndRegisterModules();
    public static final List<Map<String,Object>> contracts=new ArrayList<>();
    static final List<Map<String,Object>> rows=new ArrayList<>();
    static final List<String> gaps=new ArrayList<>();
    static final IdentityHashMap<Object,Origin> origins=new IdentityHashMap<>();
    static final IdentityHashMap<Object,List<Map<String,Object>>> constructed=new IdentityHashMap<>();
    static final ThreadLocal<Deque<Call>> calls=ThreadLocal.withInitial(ArrayDeque::new);
    static boolean enabled;
    record Origin(long order,Object writer,String path,Map<String,Object> values) {}
    record Item(Object object,String path,Map<String,Object> values,Origin origin) {}
    record Call(long order,Object command,Object writer,Map<String,Item> inputs,IdentityHashMap<Object,Boolean> admitted) {}
    @SuppressWarnings("unchecked") public static void load(String path)throws Exception {
        contracts.addAll(JSON.readValue(Path.of(path).toFile(),List.class));
    }
    public static void start(){rows.clear();gaps.clear();origins.clear();constructed.clear();calls.remove();enabled=true;}
    static long add(String kind,Map<String,Object> data){long n=rows.size();rows.add(Map.of("order",n,"kind",kind,"data",data));return n;}
    static Object writer(){return ImpactWriterContext.current().orElse(null);}
    static Map<String,Object> map(Object... values){var m=new LinkedHashMap<String,Object>();for(int i=0;i<values.length;i+=2)m.put((String)values[i],values[i+1]);return m;}
    public static void begin(Object command){if(!enabled)return;try{
        var inputs=items(command);long n=add("COMMAND_INPUT",map("command",command.getClass().getName(),"writer",writer(),"values",serialItems(inputs)));
        calls.get().push(new Call(n,command,writer(),inputs,new IdentityHashMap<>()));
    }catch(Throwable e){gap(e);}}
    public static void inbound(Object command){if(!enabled||calls.get().isEmpty())return;try{
        Call call=calls.get().peek();
        if(!command.getClass().equals(call.command().getClass()))throw new IllegalStateException("Transport command type mismatch");
        var received=items(command);
        for(var entry:received.entrySet()){
            Item from=call.inputs().get(entry.getKey()),to=entry.getValue();
            if(from==null||!from.object().getClass().equals(to.object().getClass())||!from.values().equals(to.values()))throw new IllegalStateException("Transport projection mismatch at "+entry.getKey());
        }
        if(!received.keySet().equals(call.inputs().keySet()))throw new IllegalStateException("Transport paths changed");
        for(var entry:received.entrySet()){
            Item from=call.inputs().get(entry.getKey()),to=entry.getValue();call.admitted().put(to.object(),true);
            if(from.origin()!=null){origins.put(to.object(),from.origin());add("TRANSPORT_LINK",map("call",call.order(),"path",entry.getKey(),"cloned",from.object()!=to.object(),"readOrder",from.origin().order()));}
        }
    }catch(Throwable e){gap(e);}}
    public static void end(Object result,Throwable failure){if(!enabled||calls.get().isEmpty())return;try{
        Call call=calls.get().pop();if(failure!=null)return;
        var returned=items(result);long n=add("RESPONSE",map("call",call.order(),"writer",call.writer(),"command",call.command().getClass().getName(),"values",serialItems(returned)));
        for(var i:returned.values())origins.put(i.object(),new Origin(n,call.writer(),i.path(),i.values()));
    }catch(Throwable e){gap(e);}}
    @SuppressWarnings("unchecked") public static void copied(Object target,Object[] args){if(!enabled||args.length!=1||args[0]==null||calls.get().isEmpty())return;try{
        Object source=args[0];Origin o=origins.get(source);if(o==null||!calls.get().peek().admitted().containsKey(source))return;
        for(var c:contracts){
            if(!c.get("sourceType").equals(source.getClass().getName())||!c.get("targetType").equals(target.getClass().getName()))continue;
            Map<String,String> fields=(Map<String,String>)c.get("fields");var values=new LinkedHashMap<String,Object>();
            for(var f:fields.entrySet()) {
                Object input=field(source,f.getKey()),output=field(target,f.getValue());
                if(Objects.equals(input,o.values().get(f.getKey()))&&Objects.equals(input,output))values.put(f.getValue(),input);
            }
            long n=add("CONSTRUCTOR_COPY",map("call",calls.get().peek().order(),"readOrder",o.order(),"readWriter",o.writer(),"readPath",o.path(),
                "targetType",c.get("targetType"),"sourceType",c.get("sourceType"),"keyField",c.get("targetKey"),"key",field(target,(String)c.get("targetKey")),"values",values,"writer",writer()));
            constructed.computeIfAbsent(target,k->new ArrayList<>()).add(map("copyOrder",n,"contract",c,"values",values));
        }
    }catch(Throwable e){gap(e);}}
    @SuppressWarnings("unchecked") public static void registered(Object value){if(!enabled||!(value instanceof Aggregate a))return;try{
        walk(value,"$",new IdentityHashMap<>(),0,(object,path)->{
            for(var copy:constructed.getOrDefault(object,List.of())) {
                var c=(Map<String,Object>)copy.get("contract");var values=(Map<String,Object>)copy.get("values");
                var retained=new LinkedHashMap<String,Object>();values.forEach((f,v)->{if(Objects.equals(field(object,f),v))retained.put(f,v);});
                add("REGISTERED_COPY",map("copyOrder",copy.get("copyOrder"),"aggregateId",a.getAggregateId(),"version",a.getVersion(),"path",path,"values",retained,"writer",writer()));
            }
        });
    }catch(Throwable e){gap(e);}}
    public static void committed(ImpactEvidence.AggregateSnapshot a,ImpactEvidence.Writer w){if(enabled)add("COMMITTED_WRITE",map("aggregate",a,"writer",w));}
    public static void dump(Path path)throws Exception {enabled=false;JSON.writerWithDefaultPrettyPrinter().writeValue(path.toFile(),map("schemaVersion","inferred-copy-proof.v1","contracts",contracts,"events",rows,"gaps",gaps));}
    static void gap(Throwable e){gaps.add(e.getClass().getSimpleName()+": "+e.getMessage());}
    static Map<String,Object> serialItems(Map<String,Item> items){var m=new TreeMap<String,Object>();items.forEach((p,i)->m.put(p,map("type",i.object().getClass().getName(),"values",i.values(),"readOrder",i.origin()==null?null:i.origin().order())));return m;}
    @SuppressWarnings("unchecked") static Map<String,Item> items(Object root){var result=new TreeMap<String,Item>();
        walk(root,"$",new IdentityHashMap<>(),0,(o,path)->{
            var fields=new TreeSet<String>();for(var c:contracts)if(c.get("sourceType").equals(o.getClass().getName()))fields.addAll(((Map<String,String>)c.get("fields")).keySet());
            if(fields.isEmpty())return;var values=new TreeMap<String,Object>();for(String f:fields)values.put(f,field(o,f));
            if(result.put(path,new Item(o,path,values,origins.get(o)))!=null)throw new IllegalStateException("Duplicate graph path");
        });return result;
    }
    interface Visit{void accept(Object value,String path);}
    static void walk(Object o,String path,IdentityHashMap<Object,Boolean> seen,int depth,Visit visit){
        if(o==null||scalar(o.getClass())||seen.put(o,true)!=null)return;
        try {
        if(depth>10)throw new IllegalStateException("Object graph depth exceeds experiment limit");
        if(o instanceof Collection<?> collection){
            var keys=new HashSet<String>();int index=0;
            for(Object child:collection){String key=key(child);if(key==null){if(o instanceof List<?>)key="index="+index;else throw new IllegalStateException("Unkeyed collection");}
                if(!keys.add(key))throw new IllegalStateException("Duplicate collection identity");walk(child,path+"["+key+"]",seen,depth+1,visit);index++;}return;
        }
        String pkg=o.getClass().getPackageName();
        if(pkg.startsWith("java.")||pkg.startsWith("org.")||pkg.startsWith("com.fasterxml."))return;
        if(pkg.startsWith("pt.ulisboa.tecnico.socialsoftware.ms.")&&!(o instanceof Command)&&!(o instanceof Aggregate))return;
        visit.accept(o,path);
        for(Field f:fields(o.getClass())){
            if(Modifier.isStatic(f.getModifiers())||f.isSynthetic())continue;
            // Framework state is not a user input graph; inspect application-owned aggregate fields only.
            if(o instanceof Aggregate&&f.getDeclaringClass().getPackageName().startsWith("pt.ulisboa.tecnico.socialsoftware.ms."))continue;
            try{f.setAccessible(true);walk(f.get(o),path+"."+f.getName(),seen,depth+1,visit);}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        } finally { seen.remove(o); }
    }
    static boolean scalar(Class<?> c){return c.isPrimitive()||Enum.class.isAssignableFrom(c)||Number.class.isAssignableFrom(c)||c==String.class||c==Boolean.class||c==Character.class||c.getPackageName().startsWith("java.time");}
    static String key(Object o){if(o==null)return "null";if(scalar(o.getClass()))return "value="+o;
        for(var c:contracts){String f=c.get("sourceType").equals(o.getClass().getName())?(String)c.get("sourceKey"):c.get("targetType").equals(o.getClass().getName())?(String)c.get("targetKey"):null;
            if(f!=null)return f+"="+field(o,f);}
        // Other DTO identities use the simulator aggregate identity convention, never array position for sets.
        try{Object id=field(o,"aggregateId");return id==null?null:"aggregateId="+id;}catch(RuntimeException e){return null;}
    }
    static List<Field> fields(Class<?> c){var out=new ArrayList<Field>();for(;c!=null&&c!=Object.class;c=c.getSuperclass())out.addAll(Arrays.asList(c.getDeclaredFields()));out.sort(Comparator.comparing(Field::getName));return out;}
    static Object field(Object o,String name){for(Field f:fields(o.getClass()))if(f.getName().equals(name))try{f.setAccessible(true);return f.get(o);}catch(IllegalAccessException e){throw new IllegalStateException(e);}throw new IllegalStateException("No field "+name+" on "+o.getClass());}
}
