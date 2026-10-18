package dev.meshac;

import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** Who may use /mesh. The permission API changed in 1.21.9+, so each version family has its own copy of this file. */
public final class Perm {
	public static Predicate<CommandSourceStack> admin() { return Commands.hasPermission(Commands.LEVEL_ADMINS); }
}
