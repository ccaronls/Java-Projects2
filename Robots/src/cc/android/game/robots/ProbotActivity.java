package cc.android.game.robots;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import cc.lib.android.CCActivityBase;
import cc.lib.probot.Probot;

/**
 * Created by chriscaron on 12/7/17.
 */

public class ProbotActivity extends CCActivityBase implements View.OnClickListener, View.OnTouchListener, Runnable {

    ProbotView pv;
    ProbotListView lv;
    View actionForward;
    View actionRight;
    View actionLeft;
    View actionUTurn;
    View actionJump;
    View bPlay;
    View bStop;
    View bNext;
    View bPrevious;
    TextView tvLevel;
    TextView tvLevelName;

    TextView tvForwardCount;
    TextView tvTurnRightCount;
    TextView tvTurnLeftCount;
    TextView tvUTurnCount;
    TextView tvJumpCount;

    View [] actions;
    TextView [] actionCounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.probotview);
        pv = (ProbotView)findViewById(R.id.probotView);
        lv = (ProbotListView)findViewById(R.id.lvProgram);
        lv.setProbot(pv.probot);
        tvLevel = (TextView)findViewById(R.id.tvLevel);
        tvLevelName = (TextView)findViewById(R.id.tvLevelName);
        actionForward = findViewById(R.id.ivArrowForward);
        actionRight = findViewById(R.id.ivArrowRight);
        actionLeft = findViewById(R.id.ivArrowLeft);
        actionUTurn = findViewById(R.id.ivUTurn);
        actionJump = findViewById(R.id.ivArrowJump);

        actionForward.setTag(Probot.CommandType.Advance);
        actionRight.setTag(Probot.CommandType.TurnRight);
        actionLeft.setTag(Probot.CommandType.TurnLeft);
        actionUTurn.setTag(Probot.CommandType.UTurn);
        actionJump.setTag(Probot.CommandType.Jump);

        actions = new View[] {
                actionJump, actionLeft, actionRight, actionForward, actionUTurn
        };

        tvForwardCount = (TextView)findViewById(R.id.tvArrowForwardCount);
        tvTurnRightCount = (TextView)findViewById(R.id.tvArrowRightCount);
        tvTurnLeftCount = (TextView)findViewById(R.id.tvArrowLeftCount);
        tvUTurnCount = (TextView)findViewById(R.id.tvUTurnCount);
        tvJumpCount = (TextView)findViewById(R.id.tvJumpCount);
        tvForwardCount.setTag(Probot.CommandType.Advance);
        tvTurnRightCount.setTag(Probot.CommandType.TurnRight);
        tvTurnLeftCount.setTag(Probot.CommandType.TurnLeft);
        tvUTurnCount.setTag(Probot.CommandType.UTurn);
        tvJumpCount.setTag(Probot.CommandType.Jump);

        actionCounts = new TextView[] {
                tvForwardCount, tvJumpCount, tvTurnLeftCount, tvTurnRightCount, tvUTurnCount
        };

        actionForward.setOnTouchListener(this);
        actionRight.setOnTouchListener(this);
        actionLeft.setOnTouchListener(this);
        actionUTurn.setOnTouchListener(this);
        actionJump.setOnTouchListener(this);

        bPlay = findViewById(R.id.ibPlay);
        bPlay.setOnClickListener(this);
        bStop = findViewById(R.id.ibStop);
        bStop.setOnClickListener(this);
        bPrevious = findViewById(R.id.ibPrevious);
        bPrevious.setOnClickListener(this);
        bNext = findViewById(R.id.ibNext);
        bNext.setOnClickListener(this);

        int level = getPrefs().getInt("Level", 0);
        pv.maxLevel = getPrefs().getInt("MaxLevel", 0);

        setLevel(level);
    }

    private void setLevel(int level) {
        pv.setLevel(level);
        refresh();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!pv.probot.isRunning()) {
            Probot.CommandType type = (Probot.CommandType) v.getTag();
            Probot.Command cmd = new Probot.Command(type, 0);
            lv.startDrag(v, cmd);
        }
        return true;
    }

    @Override
    public void run() {
        tvLevel.setText(String.valueOf(pv.probot.getLevelNum()+1));
        tvLevelName.setText(pv.probot.level.label);
        if (pv.probot.isRunning()) {
            bPrevious.setEnabled(false);
            bNext.setEnabled(false);
            bPlay.setVisibility(View.GONE);
            bStop.setVisibility(View.VISIBLE);
        } else {
            bPrevious.setEnabled(pv.probot.getLevelNum() > 0);
            bNext.setEnabled(BuildConfig.DEBUG || pv.probot.getLevelNum() < pv.maxLevel);
            bPlay.setVisibility(View.VISIBLE);
            bStop.setVisibility(View.GONE);
        }
        for (View a : actions) {
            Probot.CommandType c = (Probot.CommandType)a.getTag();
            a.setVisibility(pv.probot.isCommandTypeVisible(c) ? View.VISIBLE : View.GONE);
            a.setEnabled(pv.probot.getCommandTypeNumAvaialable(c) != 0);
        }
        for (TextView tv : actionCounts) {
            Probot.CommandType c = (Probot.CommandType)tv.getTag();
            if (pv.probot.isCommandTypeVisible(c)) {
                int count = pv.probot.getCommandTypeNumAvaialable(c);
                if (count < 0) {
                    tv.setVisibility(View.GONE);
                } else {
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(String.valueOf(count));
                }
            } else {
                tv.setVisibility(View.GONE);
            }
        }
    }

    public void refresh() {
        run();
    }

    public void postRefresh() {
        runOnUiThread(this);
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            /*
            case R.id.ibClear:
                pv.probot.program.clear();
                lv.notifyDataSetChanged();
                break;*/
            case R.id.ibStop:
                pv.probot.stop();
                break;
            case R.id.ibPlay:
                if (pv.probot.size() > 0) {
                    new Thread() {
                        public void run() {
                            postRefresh();
                            pv.probot.runProgram();
                            postRefresh();
                        }
                    }.start();
                }
                break;
            case R.id.ibPrevious:
                if (pv.probot.getLevelNum() > 0) {
                    setLevel(pv.probot.getLevelNum()-1);
                }
                break;
            case R.id.ibNext:
                if (pv.probot.getLevelNum() < pv.maxLevel) {
                    setLevel(pv.probot.getLevelNum()+1);
                }
                break;
        }
        refresh();
    }
}
