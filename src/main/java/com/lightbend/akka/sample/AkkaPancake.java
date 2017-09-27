package com.lightbend.akka.sample;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class AkkaPancake {

    public AkkaPancake() {

        final ActorSystem system =
                ActorSystem.create("pancakes");
        try {
            final Materializer mat =
                    ActorMaterializer.create(system);

            final List<ScoopOfBatter> orders =
                    Collections.nCopies(10, new AkkaPancake.ScoopOfBatter());

            final Source<ScoopOfBatter, NotUsed> workload =
                    Source.from(orders);

            // Takes a scoop of batter and creates a pancake with one side cooked
            final Flow<ScoopOfBatter, HalfCookedPancake, NotUsed> fryingPan1 =
                    Flow.of(ScoopOfBatter.class).map(batter -> new HalfCookedPancake());

            // Finishes a half-cooked pancake
            final Flow<HalfCookedPancake, Pancake, NotUsed> fryingPan2 =
                    Flow.of(HalfCookedPancake.class).map(halfCooked -> new Pancake());

            // With the two frying pans we can fully cook pancakes
            final Flow<ScoopOfBatter, Pancake, NotUsed> pancakeChef =
                    fryingPan1.async().via(fryingPan2.async());

            final Sink<Pancake, CompletionStage<Done>> working =
                    Sink.foreach(System.out::println);

            // Runnable flow with both ends attached (Source & Sink)
            final RunnableGraph<CompletionStage<Done>> work =
                    workload.via(pancakeChef).toMat(working, Keep.right());

            final CompletionStage<Done> futureWork = work.run(mat);

            futureWork.toCompletableFuture().get(2, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.out.println("An exception occurred: " + e.toString());
        } finally {
            system.terminate();
        }
    }


    public static final class ScoopOfBatter {

        @Override
        public String toString() {
            return "ScoopOfBatter{}";
        }
    }

    public static final class HalfCookedPancake {

        @Override
        public String toString() {
            return "HalfCookedPancake{}";
        }
    }

    public static final class Pancake {

        @Override
        public String toString() {
            return "Pancake{}";
        }
    }
}
