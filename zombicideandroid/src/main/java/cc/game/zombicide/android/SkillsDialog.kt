package cc.game.zombicide.android

import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import cc.game.zombicide.android.databinding.ViewpagerDialogBinding
import cc.lib.game.GColor
import cc.lib.zombicide.ZColor
import cc.lib.zombicide.ZPlayerName
import cc.lib.zombicide.ZSkill

/**
 * Created by Chris Caron on 8/9/21.
 */
internal class SkillsDialog(val activity: ZombicideActivity) : PagerAdapter() {
	val skills: Array<Array<Array<ZSkill>>> = Array(ZPlayerName.values().size) { plIt ->
		Array(ZColor.values().size) { colIt ->
			ZPlayerName.values()[plIt].getSkillOptions(ZColor.values()[colIt])
		}
	}

	/*
		var idx = 0
		for (pl in ZPlayerName.values()) {
			skills[idx] = arrayOfNulls(ZColor.values().size)
			labels[idx] = pl.label
			for (lvl in ZColor.values()) {
				skills[idx][lvl.ordinal] = pl.getSkillOptions(lvl)
			}
			idx++
		}
	}*/
	val labels = ZPlayerName.values().map { it.label }

	override fun getCount(): Int {
		return skills.size
	}

	override fun isViewFromObject(view: View, o: Any): Boolean {
		return view === o
	}

	override fun instantiateItem(container: ViewGroup, position: Int): Any {
		val page = View.inflate(activity, R.layout.skills_page, null)
		val title = page.findViewById<TextView>(R.id.tv_title)
		title.text = labels[position]
		val lv = page.findViewById<ListView>(R.id.lv_list)
		lv.adapter = SkillAdapter(ZPlayerName.values()[position], skills[position])
		container.addView(page)
		return page
	}

	override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
		container.removeView(`object` as View)
	}

	internal inner class Item(val name: String, val description: String?, val color: GColor?, val owned: Boolean)
	internal inner class SkillAdapter(pl: ZPlayerName, skills: Array<Array<ZSkill>>) : BaseAdapter() {
		val items: MutableList<Item> = ArrayList()
		override fun getCount(): Int {
			return items.size
		}

		override fun getItem(position: Int): Any {
			return 0
		}

		override fun getItemId(position: Int): Long {
			return 0
		}

		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
			var convertView = convertView?:View.inflate(activity, R.layout.skill_list_item, null)
			val name = convertView.findViewById<TextView>(R.id.tv_label)
			val desc = convertView.findViewById<TextView>(R.id.tv_description)
			val item = items[position]
			if (item.color == null) {
				name.setTextColor(GColor.WHITE.toARGB())
				desc.visibility = View.VISIBLE
				desc.text = item.description
				if (item.owned) {
					val content = SpannableString(item.name)
					content.setSpan(UnderlineSpan(), 0, content.length, 0)
					name.text = content
				} else {
					name.text = item.name
				}
			} else {
				name.text = item.name
				name.setTextColor(item.color.toARGB())
				desc.visibility = View.GONE
			}
			return convertView
		}

		init {
			for (lvl in ZColor.values()) {
				items.add(Item(lvl.name + " " + lvl.dangerPts + " Danger Points", null, lvl.color, false))
				for (skill in skills[lvl.ordinal]) {
					val owned = activity.game.board.getCharacterOrNull(pl)?.hasSkill(skill) == true
					items.add(Item(skill.label, skill.description, null, owned))
				}
			}
		}
	}

	init {
		// series of TABS, one for each character and then one for ALL

		val vb = ViewpagerDialogBinding.inflate(activity.layoutInflater)
		vb.viewPager.adapter = this
		if (activity.game.currentCharacter != null) {
			vb.viewPager.currentItem = activity.game.currentCharacter!!.type.ordinal
		}
		activity.newDialogBuilder().setTitle("Skills").setView(vb.root).setNegativeButton("Close", null).show()
	}
}