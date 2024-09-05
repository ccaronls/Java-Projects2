package cc.android.scorekeeper

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import cc.android.scorekeeper.databinding.ScoreKeeperBinding
import cc.android.scorekeeper.databinding.TvPointsBinding
import cc.lib.android.CCActivityBase
import cc.lib.game.Utils

class ScoreKeeper() : CCActivityBase() {
	val MAX_POINTS = 50
	val TL = 0
	val TR = 1
	val BL = 2
	val BR = 3

	enum class Cell(val foreColor: Int, val backColor: Int, val iconResource: Int) {
		WATER(R.color.blue_fore, R.color.blue_bk, R.drawable.water_icon),
		FIRE(R.color.red_fore, R.color.red_bk, R.drawable.fire_icon),
		TREE(R.color.green_fore, R.color.green_bk, R.drawable.tree_icon),
		SKULL(R.color.black_fore, R.color.black_bk, R.drawable.skull_icon),
		SUN(R.color.white_fore, R.color.white_bk, R.drawable.sun_icon)
	}

	val points = IntArray(4)
	val cells = arrayOf(Cell.WATER, Cell.FIRE, Cell.TREE, Cell.SKULL)
	lateinit var binding: ScoreKeeperBinding
	lateinit var vg: Array<ViewGroup>
	lateinit var vp: Array<ViewPager>
	lateinit var ibRemove: Array<ImageButton>
	lateinit var ibToggle: Array<ImageButton>
	lateinit var topRow: ViewGroup
	lateinit var bottomRow: ViewGroup
	val visible = BooleanArray(4)

	internal inner class ItemPagerAdapter(val index: Int) : PagerAdapter(), OnPageChangeListener {
		override fun getCount(): Int {
			return MAX_POINTS + 1
		}

		override fun isViewFromObject(view: View, `object`: Any): Boolean {
			return view === `object`
		}

		override fun instantiateItem(container: ViewGroup, position: Int): Any {
			val binding = TvPointsBinding.inflate(layoutInflater)
			val tv = binding.tvScore
			val iv = binding.ivIcon
			tv.text = position.toString()
			tv.setTextColor(resources.getColor(cells[index].foreColor))
			binding.root.setBackgroundColor(resources.getColor(cells[index].backColor))
			iv.setImageResource(cells[index].iconResource)
			iv.setColorFilter(resources.getColor(cells[index].foreColor), PorterDuff.Mode.MULTIPLY)
			container.addView(binding.root)
			return binding.root
		}

		override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
			container.removeView(`object` as View)
		}

		override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
			points[index] = position
		}

		override fun onPageSelected(position: Int) {}
		override fun onPageScrollStateChanged(state: Int) {}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		prefs.edit().putString(KEY_START_ACTIVITY, javaClass.simpleName).apply()
		binding = ScoreKeeperBinding.inflate(layoutInflater)
		binding.lifecycleOwner = this
		setContentView(binding.root)
		vg = arrayOf(binding.vgTL, binding.vgTR, binding.vgBL, binding.vgBR)
		vp = arrayOf(binding.vpTL, binding.vpTR, binding.vpBL, binding.vpBR)
		ibRemove = arrayOf(binding.ibRemoveTL, binding.ibRemoveTR, binding.ibRemoveBL, binding.ibRemoveBR)
		ibToggle = arrayOf(binding.ibToggleTL, binding.ibToggleTR, binding.ibToggleBL, binding.ibToggleBR)
		val ibPlus5 = arrayOf(binding.ibAddTL, binding.ibAddTR, binding.ibAddBL, binding.ibAddBR)
		topRow = binding.vgTopRow
		bottomRow = binding.vgBottomRow
		for (i in 0..3) {
			val index = i
			val ap = ItemPagerAdapter(i)
			vp[i].adapter = ap
			vp[i].setOnPageChangeListener(ap)
			ibRemove[i].setOnClickListener {
				vg[index].visibility = View.GONE
				visible[index] = false
				binding.ibAdd.visibility = View.VISIBLE
				updateTopBottomRow()
			}
			ibToggle[i].setOnClickListener {
				val cell = vg[index].tag as Cell
				val nxtCell = Utils.incrementValue(cell, *Cell.entries.toTypedArray())
				vg[index].tag = nxtCell.also { cells[index] = it }
				vp[index].adapter = ItemPagerAdapter(index)
				vp[index].currentItem = points.get(index)
			}
			ibPlus5[i].setOnClickListener {
				var p = points[index]
				p = Utils.clamp(5 * ((p + 5) / 5), 0, MAX_POINTS)
				points[index] = p
				vp[index].setCurrentItem(p, true)
			}
		}
		binding.ibAdd.setOnClickListener {
			for (i in visible.indices) {
				if (!visible[i]) {
					vg[i].visibility = View.VISIBLE
					visible[i] = true
					if (i == visible.size - 1) binding.ibAdd.visibility = View.GONE
					break
				}
			}
			updateTopBottomRow()
		}
		binding.bBlinds.setOnClickListener {
			startActivity(Intent(this, BlindsKeeper::class.java))
			finish()
		}
	}

	private fun updateTopBottomRow() {
		val topVisible = visible[TL] || visible[TR]
		val bottomVisible = visible[BL] || visible[BR]
		topRow.visibility = if (topVisible) View.VISIBLE else View.GONE
		bottomRow.visibility = if (bottomVisible) View.VISIBLE else View.GONE
	}

	override fun onResume() {
		super.onResume()
		val p = prefs
		points[TL] = p.getInt("pointsTL", 20)
		points[TR] = p.getInt("pointsTR", 20)
		points[BL] = p.getInt("pointsBL", 20)
		points[BR] = p.getInt("pointsBR", 20)
		visible[TL] = p.getBoolean("visibleTL", true)
		visible[TR] = p.getBoolean("visibleTR", true)
		visible[BL] = p.getBoolean("visibleBL", true)
		visible[BR] = p.getBoolean("visibleBR", true)
		vg.get(TL).tag = Cell.valueOf((p.getString("cellTL", Cell.FIRE.name)!!)).also { cells[TL] = it }
		vg.get(TR).tag = Cell.valueOf((p.getString("cellTR", Cell.WATER.name)!!)).also { cells[TR] = it }
		vg.get(BL).tag = Cell.valueOf((p.getString("cellBL", Cell.TREE.name)!!)).also { cells[BL] = it }
		vg.get(BR).tag = Cell.valueOf((p.getString("cellBR", Cell.SKULL.name)!!)).also { cells[BR] = it }
		binding.ibAdd.visibility = View.GONE
		for (i in 0..3) {
			vg.get(i).visibility = if (visible.get(i)) View.VISIBLE else View.GONE
			vp.get(i).currentItem = points.get(i)
			if (!visible[i]) binding.ibAdd.visibility = View.VISIBLE
		}
		updateTopBottomRow()
	}

	override fun onPause() {
		prefs.edit()
			.putInt("pointsTL", points[TL])
			.putInt("pointsTR", points[TR])
			.putInt("pointsBL", points[BL])
			.putInt("pointsBR", points[BR])
			.putBoolean("visibleTL", visible[TL])
			.putBoolean("visibleTR", visible[TR])
			.putBoolean("visibleBL", visible[BL])
			.putBoolean("visibleBR", visible[BR])
			.putString("cellTL", cells[TL].name)
			.putString("cellTR", cells[TR].name)
			.putString("cellBL", cells[BL].name)
			.putString("cellBR", cells[BR].name)
			.apply()
		super.onPause()
	}
}
