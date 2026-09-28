import java.nio.file.*;
import java.util.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.*;
/** Bounded pruning comparison, full normal orders and isolated controls, no compression. */
public final class PreservationProbe {
 public static void main(String[] args) throws Exception {
  GenerationComparison.main(new String[]{args[0],"quizzes",args[1],"10000","extract-only"});
  var m=GenerationComparison.model;
  var ins=InputVariantNormalizer.normalize(m.inputVariants(),cfg(ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE)).inputsBySaga();
  var selection=new ArrayList<Object>();
  for(var labels:List.of(List.of("GetCourseExecutionById","FindTournament"),List.of("GetCourseExecutionById","UpdateTournament"),List.of("UpdateTournament","FindTournament"))) {
   if(args.length>2 && !labels.equals(List.of("GetCourseExecutionById","UpdateTournament"))) continue;
   var names=ins.keySet().stream().filter(n->labels.contains(n.substring(n.lastIndexOf('.')+1).replace("FunctionalitySagas",""))).sorted().toList();
   var tuples=new ArrayList<List<InputVariant>>();
   for(var a:ins.get(names.get(0)))for(var b:ins.get(names.get(1))) if(Objects.equals(a.sourceClassFqn(),b.sourceClassFqn())&&Objects.equals(a.sourceMethodName(),b.sourceMethodName()))tuples.add(List.of(a,b));
   tuples.sort(Comparator.comparing(t->t.get(0).deterministicId()+t.get(1).deterministicId()));
   var tuple=tuples.get(args.length>2?Integer.parseInt(args[2]):0); var label=String.join("-",labels);
   selection.add(Map.of("label",label,"inputs",tuple,"selection","Lexicographic input-ID pair from the same source class and method; tuple index recorded in invocation"));
   for(var strategy:ScenarioGeneratorConfig.GenerationStrategy.values()) {
    var generated=ScenarioGenerator.generate(m.sagaDefinitions(),tuple,m.eventConsequenceDefinitions(),m.sourceSetupPlanBindings(),m.aggregateKeyInputEvidence(),cfg(strategy));
    var result=new EagerFaultScenarioGenerationResult(generated,10000,List.of(),generated.workloadPlans().stream().map(EagerFaultScenarioGenerator::evaluateMaterializability).toList(),List.of());
    new ExecutableArtifactWriter().write(m,"quizzes",result,Path.of(args[1],label,strategy.name()));
   }
  }
  GenerationComparison.write("selection.json",selection);
 }
 static ScenarioGeneratorConfig cfg(ScenarioGeneratorConfig.GenerationStrategy s) {
  return new ScenarioGeneratorConfig(true,s,ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,true,2,10000,10000,10000,true,ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,20092026L,100000,1);
 }
}
