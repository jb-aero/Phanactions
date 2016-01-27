package io.github.jbaero.factions;

import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.abstraction.MCLocation;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.ObjectGenerator;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.exceptions.CRE.CREThrowable;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.P;
import com.massivecraft.factions.iface.RelationParticipator;
import com.massivecraft.factions.integration.Worldguard;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * FHelper, 8/11/2015 9:00 PM
 *
 * @author jb_aero
 */
public class FHelper {


	public static FLocation loc(MCLocation loc) {
		return new FLocation(loc.getWorld().getName(), loc.getChunk().getX(), loc.getChunk().getZ());
	}

	/**
	 * First checks if the arg is an integer, and if it is it tries to match to a Faction id. Next it tries to match a
	 * UUID, which would return a player. Failing that, it attempts to match a Faction by exact name, and then a User by
	 * exact name.
	 *
	 * @param arg The argument from which to derive the faction or player.
	 * @param t
	 *
	 * @return Faction or Player if found, null if not
	 */
	public static RelationParticipator factionOrPlayer(Construct arg, Target t) {
		if (arg instanceof CArray) {
			return Board.getInstance().getFactionAt(loc(ObjectGenerator.GetGenerator().location(arg, null, t)));
		} else if (arg instanceof CInt) {
			return Factions.getInstance().getFactionById(arg.val());
		} else {
			try {
				return FPlayers.getInstance().getById(Static.GetUUID(arg, t).toString());
			} catch (ConfigRuntimeException cre) {
				Faction faction = Factions.getInstance().getByTag(arg.val());
				if (faction != null) {
					return faction;
				}
				try {
					return FPlayers.getInstance().getById(Static.GetUser(arg, t).getUniqueID().toString());
				} catch (ConfigRuntimeException cre2) {
					return null;
				}
			}
		}
	}

	public static boolean playerCanBuildDestroyBlock(FPlayer player, MCLocation location) {
		Player bpl = player.getPlayer();
		Location bloc = (Location) location.getHandle();

		if (Conf.playersWhoBypassAllProtection.contains(player.getName())) {
			return true;
		}

		if (player.isAdminBypassing()) {
			return true;
		}

		FLocation loc = loc(location);
		Faction otherFaction = Board.getInstance().getFactionAt(loc);

		if (otherFaction.isNone()) {
			if (Conf.worldGuardBuildPriority && Worldguard.playerCanBuild(bpl, bloc)) {
				return true;
			}

			if (!Conf.wildernessDenyBuild
					|| Conf.worldsNoWildernessProtection.contains(location.getWorld().getName())) {
				return true; // This is not faction territory. Use whatever you like here.
			}

			return false;
		} else if (otherFaction.isSafeZone()) {
			if (Conf.worldGuardBuildPriority && Worldguard.playerCanBuild(bpl, bloc)) {
				return true;
			}

			if (!Conf.safeZoneDenyBuild || bpl.hasPermission(Permission.MANAGE_SAFE_ZONE.node)) {
				return true;
			}

			return false;
		} else if (otherFaction.isWarZone()) {
			if (Conf.worldGuardBuildPriority && Worldguard.playerCanBuild(bpl, bloc)) {
				return true;
			}

			if (!Conf.warZoneDenyBuild || bpl.hasPermission(Permission.MANAGE_WAR_ZONE.node)) {
				return true;
			}

			return false;
		}
		if (P.p.getConfig().getBoolean("hcf.raidable", false)
				&& otherFaction.getLandRounded() >= otherFaction.getPowerRounded()) {
			return true;
		}

		Faction myFaction = player.getFaction();
		Relation rel = myFaction.getRelationTo(otherFaction);
		boolean online = otherFaction.hasPlayersOnline();
		boolean deny = rel.confDenyBuild(online);

		// cancel building/destroying in other territory?
		if (deny) {
			return false;
		}

		// Also cancel and/or cause pain if player doesn't have ownership rights for this claim
		if (Conf.ownedAreasEnabled
				&& (Conf.ownedAreaDenyBuild || Conf.ownedAreaPainBuild)
				&& !otherFaction.playerHasOwnershipRights(player, loc)) {
			if (Conf.ownedAreaDenyBuild) {
				return false;
			}
		}

		return true;
	}

	public static boolean factionCanBuildDestroyBlock(Faction faction, MCLocation location) {
		FLocation loc = loc(location);
		Faction otherFaction = Board.getInstance().getFactionAt(loc);

		if (otherFaction.isNone()) {
			return !Conf.wildernessDenyBuild
					|| Conf.worldsNoWildernessProtection.contains(location.getWorld().getName());
		} else if (otherFaction.isSafeZone()) {
			return !Conf.safeZoneDenyBuild;
		} else if (otherFaction.isWarZone()) {
			return !Conf.warZoneDenyBuild;
		}
		if (P.p.getConfig().getBoolean("hcf.raidable", false)
				&& otherFaction.getLandRounded() >= otherFaction.getPowerRounded()) {
			return true;
		}

		Relation rel = faction.getRelationTo(otherFaction);
		boolean online = otherFaction.hasPlayersOnline();
		boolean deny = rel.confDenyBuild(online);

		// cancel building/destroying in other territory?
		if (deny) {
			return false;
		}

		// Also cancel and/or cause pain if player doesn't have ownership rights for this claim
		return !(Conf.ownedAreasEnabled && Conf.ownedAreaDenyBuild);
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

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[0];
		}
	}
}
