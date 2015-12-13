package udea.edu.co.udealarm;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private TextView decibelsTx;
    private ProgressBar barDB;

    static final int MY_MSG = 1;
    static final int MAXOVER_MSG = 2;
    static final int ERROR_MSG = -1;

    Boolean mMode = false; // false -> fast , true -> slow
    Boolean mCalib = false;
    Boolean mLog = false;
    Boolean mMax = false;
    SoundMeterEngine mEngine = null;
    Context mContext = MainActivity.this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        decibelsTx = (TextView)findViewById(R.id.decibelsLB);
        barDB = (ProgressBar)findViewById(R.id.progress_bar);

        start_meter();
    }

    @Override
    public void onResume() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onResume();
    }

    @Override
    protected void onPause() {

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stop_meter();
        this.finish();
        super.onPause();
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stop_meter();
        this.finish();
        super.onDestroy();
    }

    /********** Implementation ********/
    public void setMeterMode(String mode) {
        mEngine.setMode(mode);
    }

    /**
     * Starts the SPL Meter
     */
    public void start_meter() {
        mCalib = false;
        mMax = false;
        mLog = false;
        mMode = false;
        mEngine = new SoundMeterEngine(mhandle, mContext);
        mEngine.start_engine();
    }

    /**
     * Stops the SPL Meter
     */
    public void stop_meter() {
        mEngine.stop_engine();
    }

    /**
     * Handler for displaying messages
     */
    public Handler mhandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MY_MSG :
                    decibelsTx.setText("" + msg.obj + "dB");
                    double barValue = (100*(double)msg.obj)/(150);
                    barDB.setProgress((int) barValue);
                    break;
                case MAXOVER_MSG :
                    mMax = false;
                    //handle_mode_display();
                    //mSplMaxButton.setTextColor(Color.parseColor("#6D7B8D"));
                    break;
                case ERROR_MSG:
                    Toast.makeText(
                            mContext,
                            "Error " + msg.obj, Toast.LENGTH_LONG).show();
                    stop_meter();
                    break;
                default :
                    super.handleMessage(msg);
                    break;
            }
        }

    };
}
