package io.github.jbaero.factions;

import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.abstraction.MCLocation;
import com.laytonsmith.abstraction.MCPlayer;
import com.laytonsmith.abstraction.MCWorld;
import com.laytonsmith.abstraction.StaticLayer;
import com.laytonsmith.abstraction.bukkit.BukkitMCLocation;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.ObjectGenerator;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CDouble;
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.CommandHelperEnvironment;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.functions.Exceptions;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.cmd.CmdCreate;
import com.massivecraft.factions.listeners.FactionsPlayerListener;

/**
 * FactionsPlugin, 8/8/2015 10:22 PM
 *
 * @author jb_aero
 */
public class FactionsPlugin {

	public static String docs() {
		return "Provides various methods for hooking into Factions.";
	}

	public static FLocation loc(MCLocation loc) {
		return new FLocation(loc.getWorld().getName(), loc.getChunk().getX(), loc.getChunk().getZ());
	}

	public static MCLocation locThisIsWrongDoNotUse(FLocation loc) {
		MCWorld world = Static.getWorld(loc.getWorldName(), Target.UNKNOWN);
		int y = world.getHighestBlockAt((int) loc.getX(), (int) loc.getZ()).getY();
		return StaticLayer.GetConvertor().GetLocation(world, loc.getX(), y, loc.getZ(), 0, 0);
	}

	public static abstract class FactionFunction extends AbstractFunction {

		@Override
		public boolean isRestricted() {
			return true;
		}

		@Override
		public Version since() {
			return CHVersion.V3_3_1;
		}

		@Override
		public Boolean runAsync() {
			return false;
		}
	}

	@api
	public static class get_faction extends FactionFunction {

		@Override
		public Exceptions.ExceptionType[] thrown() {
			return new Exceptions.ExceptionType[0];
		}

		@Override
		public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
			MCPlayer pl = null;
			Faction faction = null;
			if (args.length == 0) {
				pl = ((CommandHelperEnvironment)env.getEnv(CommandHelperEnvironment.class)).GetPlayer();
				if (pl == null) {
					throw new ConfigRuntimeException("A player context was expected for " + getName(),
							Exceptions.ExceptionType.PlayerOfflineException, t);
				}
			} else {
				if (args[0] instanceof CArray) {
					FLocation loc = loc(ObjectGenerator.GetGenerator().location(args[0], null, t));
					faction = Board.getInstance().getFactionAt(loc);
				} else {
					pl = Static.GetPlayer(args[0], t);
				}
			}
			if (pl != null) {
				faction = FPlayers.getInstance().getById(pl.getUniqueId().toString()).getFaction();
			}
			if (faction == null) {
				faction = Factions.getInstance().getWilderness();
			}
			return new CString(faction.getTag(), t);
		}

		@Override
		public String getName() {
			return "get_faction";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{0, 1};
		}

		@Override
		public String docs() {
			return "FactionName {[player | locationarray]} Returns the faction a player is in or the faction that owns"
					+ " the specified location. If no argument is given, the current player is used.";
		}
	}

	@api
	public static class get_factions extends FactionFunction {

		@Override
		public Exceptions.ExceptionType[] thrown() {
			return new Exceptions.ExceptionType[0];
		}

		@Override
		public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
			CArray ret = new CArray(t);
			for (Faction f : Factions.getInstance().getAllFactions()) {
				ret.push(new CString(f.getTag(), t));
			}
			return ret;
		}

		@Override
		public String getName() {
			return "get_factions";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{0};
		}

		@Override
		public String docs() {
			return "array {} Returns a list of all factions on the server.";
		}
	}

	@api
	public static class faction_finfo extends FactionFunction {

		@Override
		public Exceptions.ExceptionType[] thrown() {
			return new Exceptions.ExceptionType[0];
		}

		@Override
		public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
			Faction faction = Factions.getInstance().getBestTagMatch(args[0].val());
			CArray ret = CArray.GetAssociativeArray(t);
			ret.set("name", new CString(faction.getTag(), t), t);
			ret.set("id", new CString(faction.getId(), t), t);
			ret.set("description", new CString(faction.getDescription(), t), t);
			ret.set("kills", new CInt(faction.getKills(), t), t);
			ret.set("deaths", new CInt(faction.getDeaths(), t), t);
			ret.set("power", new CDouble(faction.getPower(), t), t);
			ret.set("home", ObjectGenerator.GetGenerator().location(new BukkitMCLocation(faction.getHome())), t);
			ret.set("admin", new CString(faction.getFPlayerAdmin().getId(), t), t);
			ret.set("founded", new CInt(faction.getFoundedDate(), t), t);
			return ret;
		}

		@Override
		public String getName() {
			return "faction_finfo";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "FactionArray {FactionName} Returns an array of available info about a given faction.";
		}
	}

	@api
	public static class faction_pinfo extends FactionFunction {

		@Override
		public Exceptions.ExceptionType[] thrown() {
			return new Exceptions.ExceptionType[0];
		}

		@Override
		public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
			FPlayer pl = FPlayers.getInstance().getById(Static.GetPlayer(args[0], t).getUniqueId().toString());
			CArray ret = CArray.GetAssociativeArray(t);
			ret.set("tag", pl.getTag());
			ret.set("title", pl.getTitle());
			ret.set("kills", new CInt(pl.getKills(), t), t);
			ret.set("deaths", new CInt(pl.getDeaths(), t), t);
			ret.set("power", new CDouble(pl.getPower(), t), t);
			return ret;
		}

		@Override
		public String getName() {
			return "faction_pinfo";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "FactionPlayerArray {player} Returns faction-related info about the given player.";
		}
	}

	@api
	public static class faction_relation extends FactionFunction {

		@Override
		public Exceptions.ExceptionType[] thrown() {
			return new Exceptions.ExceptionType[0];
		}

		@Override
		public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
			Faction a = Factions.getInstance().getBestTagMatch(args[0].val());
			Faction b = Factions.getInstance().getBestTagMatch(args[0].val());
			return new CString(a.getRelationTo(b).toString(), t);
		}

		@Override
		public String getName() {
			return "faction_relation";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		@Override
		public String docs() {
			return "string {factionA, factionB} Given two factions, returns the relationship between them.";
		}
	}
}
