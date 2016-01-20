package udea.edu.co.udealarm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    /****** UI components ******/
    private TextView decibelsTx;
    private ProgressBar barDB;
    private Button upCalBtn;
    private Button downCalBtn;
    private Button saveCalBtn;
    private Button startBtn;
    private Button stopBtn;
    private TextView txtAlertStatus;
    /**************************/
    MediaPlayer mp;
    /* constants */
    private static final String LOG_TAG = "UdeAlarm";

    static final int MY_MSG = 1;
    static final int MAXOVER_MSG = 2;
    static final int ERROR_MSG = -1;

    Boolean mMode = false; // false -> fast , true -> slow
    Boolean mCalib = false;
    Boolean mLog = false;
    Boolean mMax = false;
    Boolean mRunning = false;

    private int mTickCount = 0;
    private int mHitCount =0;

    private int mHitMax;

    SoundMeterEngine mEngine = null;
    Context mContext = MainActivity.this;

    /** config state **/
    private int mThreshold;

    /****** Menu context ******/
    static int PREFERENCES_GROUP_ID = 0;
    static final int CALIBRATE_OPTION = 3;
    static final int ABOUT_OPTION = 4;

    /***************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.calibrationLayout).setVisibility(View.INVISIBLE);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mp = MediaPlayer.create(this, R.raw.alarm);

        decibelsTx = (TextView)findViewById(R.id.txtLevel);
        barDB = (ProgressBar)findViewById(R.id.progress_bar);

        upCalBtn = (Button)findViewById(R.id.btnMoreTolerance);
        downCalBtn = (Button)findViewById(R.id.btnLessTolerance);
        saveCalBtn = (Button)findViewById(R.id.btnEndTolerance);

        txtAlertStatus = (TextView) findViewById(R.id.txtAlertStatus);

        startBtn=(Button)findViewById(R.id.btnStart);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAlarm();
            }
        });

        stopBtn=(Button)findViewById(R.id.btnStop);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAlarm();
            }
        });
    }

    @Override
    public void onResume() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onResume();
        readApplicationPreferences();
    }

    @Override
    protected void onPause() {
        super.onPause();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onStop() {
        super.onStop();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stopAlarm();
    }

    /********** Implementation ********/
    public void setMeterMode(String mode) {
        mEngine.setMode(mode);
    }


    /**
     * Handler for displaying messages
     */
    public Handler mhandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MY_MSG :

                    double amp = (double)msg.obj;

                    if ((amp > mThreshold) && !mCalib) {
                        mHitCount++;
                        if (mHitCount > mHitMax){
                            //Intentos superados
                            showMessage("Limite de ruido superado reiteradamente");
                            mp.start();
                            stopAlarm();
                        }
                    }

                    decibelsTx.setText("" + msg.obj + "dB");
                    double barValue = (100*(double)msg.obj)/(120);
                    barDB.setProgress((int) barValue);


                    break;
                case MAXOVER_MSG :
                    mMax = false;
                    break;
                case ERROR_MSG:
                    Toast.makeText(
                            mContext,
                            "Error " + msg.obj, Toast.LENGTH_LONG).show();
                    break;
                default :
                    super.handleMessage(msg);
                    break;
            }
        }

    };

    /**
     * Create the option Menu's
     *
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    public boolean onCreateOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        menu.add(PREFERENCES_GROUP_ID, CALIBRATE_OPTION, 0, "Calibración").setIcon(
                android.R.drawable.ic_menu_revert);
        menu.add(PREFERENCES_GROUP_ID, ABOUT_OPTION, 0, "Opciones").setIcon(
                android.R.drawable.ic_menu_help);


        return true;
    }

    /**
     * Call back function when an menu option is selected.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CALIBRATE_OPTION :

                if (mCalib){
                    showMessage("Calibración no guardada.");
                    mEngine.stop_engine();
                    decibelsTx.setText("0dB");
                    findViewById(R.id.lytButtons).setVisibility(View.VISIBLE);
                }else if (mRunning){
                    showMessage("Finalice el monitoreo para realizar la calibración");
                    break;
                }

                calibrateAction();
                break;
            case ABOUT_OPTION :

                if (mCalib || mRunning){
                    showMessage("Por favor termine el proceso actual para continuar con esta opción.");
                    break;
                }

                about();
                break;
        }
        return true;
    }


    /**
     * Iniciar analisis
     */
    public void startAlarm(){

        //mCalib = false;
        txtAlertStatus.setText(R.string.alerting_status_on);
        txtAlertStatus.setTextColor(Color.GREEN);
        mMax = false;
        mLog = false;
        mMode = false;
        mHitCount = 0;
        mTickCount = 0;

        mEngine = new SoundMeterEngine(mhandle, mContext);
        mEngine.start_engine();

        mRunning = true;
    }

    /**
     * Parar analisis
     */
    public void stopAlarm(){



        if (mRunning){
            mEngine.stop_engine();
            mRunning = false;
            txtAlertStatus.setText(R.string.alerting_status_off);
            txtAlertStatus.setTextColor(Color.GRAY);
            decibelsTx.setText("0dB");
        }

    }

    /**
     * Calibrar analisis
     */
    public void calibrateAction(){

        if (mCalib){
            findViewById(R.id.calibrationLayout).setVisibility(View.INVISIBLE);
            mCalib = false;
            findViewById(R.id.lytButtons).setVisibility(View.VISIBLE);
            decibelsTx.setText("0dB");

        }else{

            mEngine = new SoundMeterEngine(mhandle, mContext);
            mEngine.start_engine();
            showMessage("Está en modo calibración\nAjuste los parámetros y guarde los cambios " +
                    "pulsando el botón GUARDAR para continuar");

            findViewById(R.id.calibrationLayout).setVisibility(View.VISIBLE);
            findViewById(R.id.lytButtons).setVisibility(View.INVISIBLE);
            mCalib = true;


        }
    }


    public void moreTolerance(View view){
        mEngine.calibUp();
    }

    public void lessTolerance(View view){

        mEngine.calibDown();
    }

    public void endCalibrateOptions(View view){

        mEngine.storeCalibvalue();

        mEngine.stop_engine();

        showMessage("Calibración guardada.");

        calibrateAction();

    }

    /**
     * Mostrar acerca de
     */
    public void about(){

        Intent i = new Intent(MainActivity.this, AppPreferences.class);
        startActivity(i);

    }

    /**
     * Check app preferences
     */
    private void readApplicationPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mHitMax = Integer.parseInt(prefs.getString("hitsMax","60"));
        mThreshold = Integer.parseInt(prefs.getString("threshold","10"));

        Log.i(LOG_TAG, "hitMax=" + mHitMax);
        Log.i(LOG_TAG, "threshold=" + mThreshold);
    }

    private void showMessage(String msg){

        showMessageWithDuration(msg, Toast.LENGTH_LONG);

    }

    private  void showMessageWithDuration(String msg, int duration){
        Toast.makeText(mContext, msg, duration).show();
    }
}
