package cc.game.zombicide.android

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import cc.lib.game.GRectangle
import cc.lib.zombicide.ZQuests
import kotlin.math.roundToInt

/**
 * Created by Chris Caron on 8/22/21.
 */
internal class NewGameChooseQuestDialog(val activity: ZombicideActivity, val allQuests: List<ZQuests>, playable: MutableSet<ZQuests>) : PagerAdapter() {
	val playable: Set<ZQuests>
	var firstPage = 0
	val dialog: Dialog
	override fun getCount(): Int {
		return allQuests.size
	}

	override fun isViewFromObject(view: View, o: Any): Boolean {
		return view === o
	}

	override fun instantiateItem(container: ViewGroup, position: Int): Any {
		val q = allQuests[position]
		val content = View.inflate(activity, R.layout.choose_quest_dialog_item, null)
		val tiles = q.load().tiles
		var bm: Bitmap? = null
		if (tiles.isNotEmpty()) {
			val rect = GRectangle()
			val imageDim = 64f
			for (tile in tiles) {
				rect.addEq(tile.quadrant)
			}
			rect.scaleDimension(imageDim)
			bm = Bitmap.createBitmap(rect.width.roundToInt(), rect.height.roundToInt(), Bitmap.Config.ARGB_8888)
			val c = Canvas(bm)
			c.scale(imageDim, imageDim)
			for (tile in tiles) {
				try {
					val t = BitmapFactory.decodeStream(container.context.assets.open("ztile_" + tile.id + ".png"))
					c.save()
					c.translate(tile.quadrant.x + 1.5f, tile.quadrant.y + 1.5f)
					c.rotate(tile.orientation.toFloat())
					c.translate(-1.5f, -1.5f)
					val src = Rect(0, 0, t.width, t.height)
					val dst = Rect(0, 0, 3, 3)
					c.drawBitmap(t, src, dst, null)
					t.recycle()
					c.restore()
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
		}
		val tv_body: TextView
		val iv_board: ImageView
		if (bm == null || bm.width > bm.height) {
			// use horz
			tv_body = content.findViewById(R.id.tv_body_horz)
			iv_board = content.findViewById(R.id.iv_board_horz)
		} else {
			// use vert
			tv_body = content.findViewById(R.id.tv_body_vert)
			iv_board = content.findViewById(R.id.iv_board_vert)
		}
		val title = content.findViewById<TextView>(R.id.tv_title)
		val lockedOverlay = content.findViewById<ImageView>(R.id.lockedOverlay)
		title.text = q.displayName
		tv_body.text = q.description
		if (bm != null) iv_board.setImageBitmap(bm)
		if (playable.contains(q)) {
			lockedOverlay.visibility = View.INVISIBLE
			content.setOnClickListener {
				dialog.dismiss()
				activity.showChooseGameModeDialog(q)
			}
		} else {
			lockedOverlay.visibility = View.VISIBLE
			content.setOnClickListener(null)
		}
		container.addView(content)
		return content
	}

	override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
		container.removeView(obj as View)
	}

	companion object {
		val TAG = NewGameChooseQuestDialog::class.java.simpleName
	}

	init {
		this.playable = playable
		for (q in allQuests) {
			if (!playable.contains(q)) {
				playable.add(q)
				break
			}
			firstPage = (firstPage + 1).coerceIn(allQuests.indices)
		}
		val view = View.inflate(activity, R.layout.viewpager_dialog, null)
		val pager: ViewPager = view.findViewById(R.id.view_pager)
		pager.adapter = this
		pager.currentItem = firstPage
		dialog = activity.pushDialog().setTitle(R.string.popup_title_choose_quest)
			.setView(view).setPositiveButton(R.string.popup_button_next) { dialog, which ->
				val q = allQuests[pager.currentItem]
				if (playable.contains(q)) {
					activity.showNewGameDialogChooseDifficulty(q)
				} else {
					Toast.makeText(activity, "Quest Locked", Toast.LENGTH_LONG).show()
				}
			}
			.setNegativeButton(R.string.popup_button_back) { dialog, which -> activity.showNewGameDialog() }
			.show()
	}
}