package net.sf.l2j.gameserver.network.clientpackets;

import static net.sf.l2j.gameserver.model.itemcontainer.PcInventory.ADENA_ID;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.enums.StatusType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Folk;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.ItemContainer;
import net.sf.l2j.gameserver.model.itemcontainer.PcWarehouse;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.EnchantResult;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.StatusUpdate;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class SendWarehouseDepositList extends L2GameClientPacket
{
	private static final int BATCH_LENGTH = 8; // length of one item
	
	private IntIntHolder[] _items = null;
	
	@Override
	protected void readImpl()
	{
		final int count = readD();
		if (count <= 0 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
			return;
		
		_items = new IntIntHolder[count];
		for (int i = 0; i < count; i++)
		{
			int objId = readD();
			int cnt = readD();
			
			if (objId < 1 || cnt < 0)
			{
				_items = null;
				return;
			}
			_items[i] = new IntIntHolder(objId, cnt);
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_items == null)
			return;
		
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		if (player.isProcessingTransaction())
		{
			player.sendPacket(SystemMessageId.ALREADY_TRADING);
			return;
		}
		
		if (player.getActiveEnchantItem() != null)
		{
			player.setActiveEnchantItem(null);
			player.sendPacket(EnchantResult.CANCELLED);
			player.sendPacket(SystemMessageId.ENCHANT_SCROLL_CANCELLED);
		}
		
		final ItemContainer warehouse = player.getActiveWarehouse();
		if (warehouse == null)
			return;
		
		final boolean isPrivate = warehouse instanceof PcWarehouse;
		
		final Folk folk = player.getCurrentFolk();
		if (folk == null || !folk.isWarehouse() || !player.getAI().canDoInteract(folk))
			return;
		
		if (!isPrivate && !player.getAccessLevel().allowTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			return;
		}
		
		// Alt game - Karma punishment
		if (!Config.KARMA_PLAYER_CAN_USE_WH && player.getKarma() > 0)
			return;
		
		// Freight price from config or normal price per item slot (30)
		final int fee = _items.length * 30;
		int currentAdena = player.getAdena();
		int slots = 0;
		
		for (IntIntHolder i : _items)
		{
			ItemInstance item = player.checkItemManipulation(i.getId(), i.getValue());
			if (item == null)
				return;
			
			// Calculate needed adena and slots
			if (item.getItemId() == ADENA_ID)
				currentAdena -= i.getValue();
			
			if (!item.isStackable())
				slots += i.getValue();
			else if (warehouse.getItemByItemId(item.getItemId()) == null)
				slots++;
		}
		
		// Item Max Limit Check
		if (!warehouse.validateCapacity(slots))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED));
			return;
		}
		
		// Check if enough adena and charge the fee
		if (currentAdena < fee || !player.reduceAdena(warehouse.getName(), fee, folk, false))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
			return;
		}
		
		// get current tradelist if any
		if (player.getActiveTradeList() != null)
			return;
		
		// Proceed to the transfer
		InventoryUpdate playerIU = new InventoryUpdate();
		for (IntIntHolder i : _items)
		{
			// Check validity of requested item
			ItemInstance oldItem = player.checkItemManipulation(i.getId(), i.getValue());
			if (oldItem == null)
				return;
			
			if (!oldItem.isDepositable(isPrivate) || !oldItem.isAvailable(player, true, isPrivate, false))
				continue;
			
			final ItemInstance newItem = player.getInventory().transferItem(warehouse.getName(), i.getId(), i.getValue(), warehouse, player, folk);
			if (newItem == null)
				continue;
			
			if (oldItem.getCount() > 0 && oldItem != newItem)
				playerIU.addModifiedItem(oldItem);
			else
				playerIU.addRemovedItem(oldItem);
		}
		
		// Send updated item list to the player
		player.sendPacket(playerIU);
		
		// Update current load status on player
		StatusUpdate su = new StatusUpdate(player);
		su.addAttribute(StatusType.CUR_LOAD, player.getCurrentWeight());
		player.sendPacket(su);
	}
}