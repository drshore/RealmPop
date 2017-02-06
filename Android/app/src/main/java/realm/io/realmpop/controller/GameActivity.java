package realm.io.realmpop.controller;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.Interval;
import org.joda.time.Period;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import realm.io.realmpop.R;
import realm.io.realmpop.model.GameModel;
import realm.io.realmpop.model.realm.Bubble;
import realm.io.realmpop.model.realm.Game;
import realm.io.realmpop.model.realm.Player;
import realm.io.realmpop.model.realm.Score;
import realm.io.realmpop.model.realm.Side;

import static realm.io.realmpop.util.RandomUtils.generateNumber;

public class GameActivity extends AppCompatActivity {

    @BindView(R.id.playerLabel1)
    public TextView player1;

    @BindView(R.id.playerLabel2)
    public TextView player2;

    @BindView(R.id.message)
    public TextView message;

    @BindView(R.id.timer)
    public TextView timerLabel;

    @BindView(R.id.bubbleBoard)
    public RelativeLayout bubbleBoard;

    private Realm realm;
    private GameModel gameModel;
    private Game challenge;

    private Player me;

    private Side mySide;
    private Side otherSide;

    private Timer timer;
    private Date startedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        ButterKnife.bind(this);

        realm = Realm.getDefaultInstance();
        gameModel = new GameModel(realm);
        me = gameModel.currentPlayer();
        me.addChangeListener(new RealmChangeListener<Player>() {
            @Override
            public void onChange(Player me) {
                if(me.getCurrentgame() == null) {
                    finish();
                }
            }
        });

        challenge = me.getCurrentgame();

        mySide = challenge.getPlayer1();
        mySide.addChangeListener(new RealmChangeListener<Side>() {
            @Override
            public void onChange(Side me) {
                update();
            }
        });

        otherSide = challenge.getPlayer2();
        otherSide.addChangeListener(new RealmChangeListener<Side>() {
            @Override
            public void onChange(Side other) {
                update();
            }
        });



        float density  = 3.5f; //TODO: Clean up magic numbers
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        final int MAX_X_MARGIN =  Math.round(outMetrics.widthPixels - (100f * density));
        final int MAX_Y_MARGIN =  Math.round(outMetrics.heightPixels - (180f * density));

        for(final Bubble bubble : mySide.getBubbles()) {
            View bubbleView = getLayoutInflater().inflate(R.layout.bubble, bubbleBoard, false);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) bubbleView.getLayoutParams();

            params.leftMargin = generateNumber(0, MAX_X_MARGIN);
            params.topMargin = generateNumber(0, MAX_Y_MARGIN);

            ((TextView) bubbleView.findViewById(R.id.bubbleValue)).setText(String.valueOf(bubble.getNumber()));
            bubbleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBubbleTap(bubble.getNumber());
                }
            });

            bubbleBoard.addView(bubbleView, params);
        }

        update();

    }

    @Override
    protected void onResume() {
        super.onResume();

        startedAt = new Date();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Interval interval = new Interval(startedAt.getTime(), new Date().getTime());
                Period period = interval.toPeriod();
                final String timerText = String.format("%02d:%02d", period.getMinutes(), period.getSeconds());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        timerLabel.setText(timerText);
                    }
                });
            }
        }, now(), 1000);

    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.cancel();
        timer = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.removeAllChangeListeners();
        realm.close();
        realm = null;
        gameModel = null;
    }

    @OnClick(R.id.exitGameLabel)
    public void exitGame() {
        if(gameModel != null && realm != null) {

            final String myId = me.getId();
            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm bgRealm) {

                    Player me = bgRealm.where(Player.class).equalTo("id", myId).findFirst();
                    Player challenger = me.getChallenger();
                    Game game = me.getCurrentgame();
                    Side s1 = game.getPlayer1();
                    Side s2 = game.getPlayer2();

                    s1.getBubbles().deleteAllFromRealm();
                    s1.deleteFromRealm();
                    s2.getBubbles().deleteAllFromRealm();
                    s2.deleteFromRealm();
                    game.deleteFromRealm();

                    if(challenger != null) {
                        challenger.setCurrentgame(null);
                        challenger.setChallenger(null);
                    }

                    me.setCurrentgame(null);
                    me.setChallenger(null);
                }
            });
        }
    }

    public void onBubbleTap(final long number) {

        Toast.makeText(this, "" + number, Toast.LENGTH_LONG).show();

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Bubble bubble = mySide.getBubbles().last();
                if(bubble != null && bubble.getNumber() == number) {
                    mySide.getBubbles().remove(bubble);
                } else {
                    message.setText("You tapped " + number + " instead of " + (bubble == null ? 0 : bubble.getNumber()));
                    mySide.setFailed(true);
                    message.setVisibility(View.VISIBLE);
                    Handler handler = new Handler(GameActivity.this.getMainLooper());
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            exitGame();
                        }
                    }, 2000);
                }
            }
        });


//        try! challenge.realm?.write {
//            if let bubble = mySide.bubbles.last, bubble.number == number {
//                mySide.bubbles.removeLast()
//            } else {
//                message.isHidden = false
//                message.text = "You tapped \(number) instead of \(mySide.bubbles.last?.number ?? 0)"
//                mySide.failed = true
//                endGame()
//            }
//        }

    }

    private Date now() {
        return new Date();
    }

    private void update() {

        player1.setText(challenge.getPlayer1().getName() + " : " + challenge.getPlayer1().getBubbles().size());
        player2.setText(challenge.getPlayer2().getName() + " : " + challenge.getPlayer2().getBubbles().size());

        if(otherSide.isFailed()) {
            message.setText("You win! Congrats");
            message.setVisibility(View.VISIBLE);
        } else if(mySide.isFailed()) {
            message.setText("You lost!");
            message.setVisibility(View.VISIBLE);
        }

        if(mySide.getBubbles().size() > 0) {

            if(mySide.getTime() == 0) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        mySide.setTime(System.currentTimeMillis());
                    }
                });
                message.setVisibility(View.VISIBLE);

            }

            if( otherSide.getTime() > 0 && mySide.getTime() > 0) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        if( otherSide.getTime() < mySide.getTime() ) {
                            mySide.setFailed(true);
                        } else {
                            otherSide.setFailed(true);
                            Score score = new Score();
                            score.setName(mySide.getName());
                            score.setTime(mySide.getTime());
                            realm.copyToRealm(score);
                        }
                    }
                });
            }

        }
    }
}

