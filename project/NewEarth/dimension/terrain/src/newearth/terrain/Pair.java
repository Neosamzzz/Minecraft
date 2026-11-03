/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain;
public record Pair<A,B>(A first,B second){public A getFirst(){return first;}public B getSecond(){return second;}public static <A,B> Pair<A,B> of(A a,B b){return new Pair<>(a,b);}}