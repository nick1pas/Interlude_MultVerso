package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.data.manager.CastleManager;
import net.sf.l2j.gameserver.data.manager.ClanHallManager;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.enums.SiegeSide;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.clanhall.ClanHall;
import net.sf.l2j.gameserver.model.clanhall.ClanHallFunction;
import net.sf.l2j.gameserver.model.entity.Siege;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.pledge.Clan;

public final class RequestRestartPoint extends L2GameClientPacket
{
	protected static final Location JAIL_LOCATION = new Location(-114356, -249645, -2984);

	protected int _requestType;

	@Override
	protected void readImpl()
	{
		_requestType = readD();
	}

	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
		{
			return;
		}

		// TODO Needed? Possible?
		if (player.isFakeDeath())
		{
			player.stopFakeDeath(true);
			return;
		}

		if (CTFEvent.isStarted() && CTFEvent.isPlayerParticipant(player.getObjectId()) || DMEvent.isStarted() && DMEvent.isPlayerParticipant(player.getObjectId()) || LMEvent.isStarted() && LMEvent.isPlayerParticipant(player.getObjectId()) || TvTEvent.isStarted() && TvTEvent.isPlayerParticipant(player.getObjectId()))
		{
			return;
		}

		if (!player.isDead())
		{
			return;
		}

		portPlayer(player);
	}

	/**
	 * Teleport the {@link Player} to the associated {@link Location}, based on _requestType.
	 * @param player : The player set as parameter.
	 */
	private void portPlayer(Player player)
	{
		final Clan clan = player.getClan();

		Location loc = null;

		// Enforce type.
		if (player.isInJail())
		{
			_requestType = 27;
		}
		else if (player.isFestivalParticipant())
		{
			_requestType = 4;
		}

		// To clanhall.
		if (_requestType == 1)
		{
			if (clan == null || !clan.hasClanHall())
			{
				return;
			}

			loc = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.CLAN_HALL);

			final ClanHall ch = ClanHallManager.getInstance().getClanHallByOwner(clan);
			if (ch != null)
			{
				final ClanHallFunction function = ch.getFunction(ClanHall.FUNC_RESTORE_EXP);
				if (function != null)
				{
					player.restoreExp(function.getLvl());
				}
			}
		}
		// To castle.
		else if (_requestType == 2)
		{
			final Siege siege = CastleManager.getInstance().getActiveSiege(player);
			if (siege != null)
			{
				final SiegeSide side = siege.getSide(clan);
				if (side == SiegeSide.DEFENDER || side == SiegeSide.OWNER)
				{
					loc = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.CASTLE);
				}
				else if (side == SiegeSide.ATTACKER)
				{
					loc = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.TOWN);
				}
				else
				{
					return;
				}
			}
			else
			{
				if (clan == null || !clan.hasCastle())
				{
					return;
				}

				loc = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.CASTLE);
			}
		}
		// To siege flag.
		else if (_requestType == 3)
		{
			loc = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.SIEGE_FLAG);
		}
		else if (_requestType == 4)
		{
			if (!player.isGM() && !player.isFestivalParticipant())
			{
				return;
			}

			loc = player.getPosition();
		}
		// To jail.
		else if (_requestType == 27)
		{
			if (!player.isInJail())
			{
				return;
			}

			loc = JAIL_LOCATION;
		}
		// Nothing has been found, use regular "To town" behavior.
		else
		{
			loc = MapRegionData.getInstance().getLocationToTeleport(player, TeleportType.TOWN);
		}

		player.setIsIn7sDungeon(false);

		if (player.isDead())
		{
			player.doRevive();
		}

		player.teleportTo(loc, 20);
	}
}