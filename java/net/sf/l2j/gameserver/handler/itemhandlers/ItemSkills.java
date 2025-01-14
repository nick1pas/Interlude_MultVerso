package net.sf.l2j.gameserver.handler.itemhandlers;

import net.sf.l2j.commons.util.ArraysUtil;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Playable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Pet;
import net.sf.l2j.gameserver.model.actor.instance.Servitor;
import net.sf.l2j.gameserver.model.entity.events.capturetheflag.CTFEvent;
import net.sf.l2j.gameserver.model.entity.events.deathmatch.DMEvent;
import net.sf.l2j.gameserver.model.entity.events.lastman.LMEvent;
import net.sf.l2j.gameserver.model.entity.events.teamvsteam.TvTEvent;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.skills.L2Skill;
import net.sf.l2j.gameserver.skills.effects.EffectTemplate;

public class ItemSkills implements IItemHandler
{
	
	private static final int[] HP_POTION_SKILL_IDS =
	{
		2031, // Lesser Healing Potion
		2032, // Healing potion
		2037 // Greater Healing Potion
	};
	
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (playable instanceof Servitor)
		{
			return;
		}
		
		final boolean isPet = playable instanceof Pet;
		final Player player = playable.getActingPlayer();
		final Creature target = playable.getTarget() instanceof Creature ? (Creature) playable.getTarget() : null;
		
		// Pets can only use tradable items.
		if (isPet && !item.isTradable())
		{
			player.sendPacket(SystemMessageId.ITEM_NOT_FOR_PETS);
			return;
		}
		
		final IntIntHolder[] skills = item.getEtcItem().getSkills();
		if (skills == null)
		{
			LOGGER.warn("{} doesn't have any registered skill for handler.", item.getName());
			return;
		}
		// Players can't use BSOE while flagged.
		if ((playable != null) && (playable.getPvpFlag() != 0) && ((item.getItemId() == 1538) ||
		
		// Blessed Scroll of Escape
			(item.getItemId() == 3958) || // Blessed Scroll of Escape (Event)
			(item.getItemId() == 5858) || // Blessed Scroll of Escape: Clan Hall
			(item.getItemId() == 5859) || // Blessed Scroll of Escape: Castle
			(item.getItemId() == 9156) || // Blessed Scroll of Escape (Event)
			(item.getItemId() == 10130) || // Blessed Scroll of Escape: Fortress
			(item.getItemId() == 13258) || // Gran Kain's Blessed Scroll of Escape
			(item.getItemId() == 13731) || // Blessed Scroll of Escape: Gludio
			(item.getItemId() == 13732) || // Blessed Scroll of Escape: Dion
			(item.getItemId() == 13733) || // Blessed Scroll of Escape: Giran
			(item.getItemId() == 13734) || // Blessed Scroll of Escape: Oren
			(item.getItemId() == 13735) || // Blessed Scroll of Escape: Aden
			(item.getItemId() == 13736) || // Blessed Scroll of Escape: Innadril
			(item.getItemId() == 13737) || // Blessed Scroll of Escape: Goddard
			(item.getItemId() == 13738) || // Blessed Scroll of Escape: Rune
			(item.getItemId() == 13739) || // Blessed Scroll of Escape: Schuttgart
			(item.getItemId() == 20583) || // Blessed Scroll of Escape (event)
			(item.getItemId() == 21195))) // Blessed Scroll of Escape
			
			for (final IntIntHolder skillInfo : skills)
			{
				if (skillInfo == null)
				{
					continue;
				}
				
				final L2Skill itemSkill = skillInfo.getSkill();
				if (itemSkill == null)
				{
					continue;
				}
				
				// No message on retail, the use is just forgotten.
				if (!itemSkill.checkCondition(playable, target, false) || playable.isSkillDisabled(itemSkill))
				{
					return;
				}
				
				if (!CTFEvent.onScrollUse(playable.getObjectId()) || !DMEvent.onScrollUse(playable.getObjectId()) || !LMEvent.onScrollUse(playable.getObjectId()) || !TvTEvent.onScrollUse(playable.getObjectId()))
				{
					playable.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
				
				// Potions and Energy Stones bypass the AI system. The rest does not.
				if (itemSkill.isPotion() || itemSkill.isSimultaneousCast())
				{
					playable.getCast().doInstantCast(itemSkill, item);
					
					if (!isPet && item.isHerb() && player.hasServitor())
					{
						player.getSummon().getCast().doInstantCast(itemSkill, item);
					}
				}
				else
				{
					playable.getAI().tryToCast(target, itemSkill, forceUse, false, (item.isEtcItem() ? item.getObjectId() : 0));
				}
				
				// Send message to owner.
				if (isPet)
				{
					player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_USES_S1).addSkillName(itemSkill));
				}
				else
				{
					// Buff icon for healing potions.
					final int skillId = skillInfo.getId();
					if (ArraysUtil.contains(HP_POTION_SKILL_IDS, skillId) && skillId >= player.getShortBuffTaskSkillId())
					{
						final EffectTemplate template = itemSkill.getEffectTemplates().get(0);
						if (template != null)
						{
							player.shortBuffStatusUpdate(skillId, skillInfo.getValue(), template.getCounter() * template.getPeriod());
						}
					}
				}
			}
	}
}