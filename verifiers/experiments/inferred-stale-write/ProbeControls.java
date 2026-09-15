package experiment.inferred;

import com.example.dummyapp.inferredcopy.*;
import java.nio.file.*;
import java.util.*;

/** Small inputs for exercising identity tracking, independently of Quizzes. Constructors are agent-instrumented. */
public class ProbeControls {
    public static class Envelope { public Set<CardInput> cards; public CardInput selected; Envelope(CardInput... values){cards=new LinkedHashSet<>(Arrays.asList(values));} }
    public static void main(String[] args)throws Exception {
        Path out=Path.of(args[0]);Files.createDirectories(out);
        for(String mode:List.of("same-instance","cloned-untracked","changed-input","transport-clone","transport-reorder","transport-alias","transport-mismatch","duplicate-key","ignored-construction")) {
            Probe.start();CardInput one=new CardInput(1,"old"),two=new CardInput(2,"other");
            Probe.begin(new Object());Probe.end(new Envelope(one,two),null);
            if(mode.equals("cloned-untracked"))one=new CardInput(1,"old");
            if(mode.equals("changed-input"))one.setCaption("changed");
            Envelope command=new Envelope(one,two);if(mode.equals("transport-alias"))command.selected=one;Probe.begin(command);
            Envelope incoming=command;
            if(mode.startsWith("transport-")) {
                CardInput x=new CardInput(1,mode.equals("transport-mismatch")?"wrong":"old"),y=new CardInput(2,"other");
                incoming=mode.equals("transport-reorder")?new Envelope(y,x):new Envelope(x,y);
                if(mode.equals("transport-alias"))incoming.selected=new CardInput(1,"old");
            }
            if(mode.equals("duplicate-key"))incoming=new Envelope(new CardInput(1,"old"),new CardInput(1,"other"));
            Probe.inbound(incoming);
            for(CardInput input:incoming.cards)new StoredCard(input);
            Probe.end(null,null);
            Probe.dump(out.resolve(mode+".json"));
        }
    }
}
