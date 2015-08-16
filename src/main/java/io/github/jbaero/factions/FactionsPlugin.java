package io.github.jbaero.factions;

import com.laytonsmith.abstraction.MCLocation;
import com.laytonsmith.abstraction.MCOfflinePlayer;
import com.laytonsmith.abstraction.bukkit.BukkitMCLocation;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.ObjectGenerator;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CDouble;
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.CommandHelperEnvironment;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.iface.RelationParticipator;

/**
 * FactionsPlugin, 8/8/2015 10:22 PM
 *
 * @author jb_aero
 */
public class FactionsPlugin {

	public static String docs() {
		return "Provides various methods for hooking into Factions.";
	}

	@api
	public static class get_faction extends FHelper.FactionFunction {

		@Override
		public ExceptionType[] thrown() {
			return new ExceptionType[0];
		}

		@Override
		public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
			MCOfflinePlayer pl = null;
			Faction faction = null;
			if (args.length == 0) {
				pl = env.getEnv(CommandHelperEnvironment.class).GetPlayer();
				if (pl == null) {
					throw new ConfigRuntimeException("A player context was expected for " + getName(),
							ExceptionType.PlayerOfflineException, t);
				}
			} else {
				if (args[0] instanceof CArray) {
					FLocation loc = FHelper.loc(ObjectGenerator.GetGenerator().location(args[0], null, t));
					faction = Board.getInstance().getFactionAt(loc);
				} else {
					pl = Static.GetUser(args[0], t);
				}
			}
			if (pl != null) {
				faction = FPlayers.getInstance().getById(pl.getUniqueID().toString()).getFaction();
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
	public static class get_factions extends FHelper.FactionFunction {

		@Override
		public ExceptionType[] thrown() {
			return new ExceptionType[0];
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
	public static class faction_finfo extends FHelper.FactionFunction {

		@Override
		public ExceptionType[] thrown() {
			return new ExceptionType[0];
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
			ret.set("home", faction.hasHome() ? ObjectGenerator.GetGenerator().location(
					new BukkitMCLocation(faction.getHome())) : CNull.NULL, t);
			FPlayer admin = faction.getFPlayerAdmin();
			ret.set("admin", admin == null ? CNull.NULL : new CString(admin.getId(), t), t);
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
	public static class faction_pinfo extends FHelper.FactionFunction {

		@Override
		public ExceptionType[] thrown() {
			return new ExceptionType[0];
		}

		@Override
		public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
			FPlayer pl = FPlayers.getInstance().getById(Static.GetUser(args[0], t).getUniqueID().toString());
			CArray ret = CArray.GetAssociativeArray(t);
			ret.set("tag", pl.getTag());
			ret.set("title", pl.getTitle());
			ret.set("kills", new CInt(pl.getKills(), t), t);
			ret.set("deaths", new CInt(pl.getDeaths(), t), t);
			ret.set("power", new CDouble(pl.getPower(), t), t);
			ret.set("role", new CString(pl.getRole().toString(), t), t);
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
	public static class faction_relation extends FHelper.FactionFunction {

		@Override
		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.NotFoundException};
		}

		@Override
		public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
			RelationParticipator a = FHelper.factionOrPlayer(args[0], t);
			RelationParticipator b = FHelper.factionOrPlayer(args[1], t);
			if (a == null) {
				throw new ConfigRuntimeException("Could not find a relatable object for argument 1.",
						ExceptionType.NotFoundException, t);
			}
			if (b == null) {
				throw new ConfigRuntimeException("Could not find a relatable object for argument 2.",
						ExceptionType.NotFoundException, t);
			}
			return new CString(a.getRelationTo(b).name(), t);
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
			return "string {factionA/playerA/locationA, factionB/playerB/locationB} Given two factions, two players,"
					+ " two locations, or any combination of the three, returns the relationship between them.";
		}
	}

	@api
	public static class faction_can_build extends FHelper.FactionFunction {

		@Override
		public ExceptionType[] thrown() {
			return new ExceptionType[0];
		}

		@Override
		public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
			RelationParticipator user = FHelper.factionOrPlayer(args[0], t);
			MCLocation loc = ObjectGenerator.GetGenerator().location(args[1], null, t);
			if (user instanceof Faction) {
				return CBoolean.get(FHelper.factionCanBuildDestroyBlock((Faction) user, loc));
			}
			if (user instanceof FPlayer) {
				return CBoolean.get(FHelper.playerCanBuildDestroyBlock((FPlayer) user, loc));
			}
			throw new ConfigRuntimeException("Could not determine a Faction or player based on arg 1.",
					ExceptionType.NotFoundException, t);
		}

		@Override
		public String getName() {
			return "faction_can_build";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		@Override
		public String docs() {
			return "boolean {Faction/Player, location} Returns whether the provided Faction or Player can build at the given location.";
		}
	}
}
