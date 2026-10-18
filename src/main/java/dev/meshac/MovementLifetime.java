package dev.meshac;
/** Movement integrators describe one living entity in one dimension, not a UUID forever. */
public final class MovementLifetime {
 private MovementLifetime() {}
 public static boolean reusable(int oldEntity,int entity,Object oldDimension,Object dimension,boolean alive){
  return alive && oldEntity==entity && java.util.Objects.equals(oldDimension,dimension);
 }
}
