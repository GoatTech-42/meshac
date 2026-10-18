package dev.meshac.veil;

/**
 * Pure decision for the anti-ESP ray: may this block stop a line of sight? No Minecraft types.
 * Why: fences, fence gates and walls keep canOcclude() == true and their visual shape defaults to the COLLISION shape
 * (1.5 blocks tall, solid 4/16 thick rails). The real model has gaps between rails and is only 1 block tall, so the ray
 * test treated a fence line as a solid wall and hid players a client can plainly see through it.
 * Rule: a block blocks only if it occludes, is not foliage, is not a "gappy" model (fence, gate, wall) and the ray
 * really crosses its shape. Slabs and stairs stay blockers on purpose: their visual shape is their true opaque geometry.
 * Doors, trapdoors, panes, bars, glass, scaffolding, cobweb have canOcclude() == false and never block.
 */
public final class RayBlocking {
	private RayBlocking() {}
	public static boolean blocks(boolean air, boolean canOcclude, boolean foliage, boolean gappyModel, boolean emptyShape, boolean rayCrossesShape) {
		if (air || !canOcclude || foliage || gappyModel || emptyShape) return false;
		return rayCrossesShape;
	}
}
