package udea.edu.co.udealarm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private TextView modeTx;
    /**************************/

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
    static final int STOP_OPTION = 2;
    static final int START_OPTION = 1;

    /***************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        decibelsTx = (TextView)findViewById(R.id.decibelsLB);
        barDB = (ProgressBar)findViewById(R.id.progress_bar);

        upCalBtn = (Button)findViewById(R.id.upButton);
        downCalBtn = (Button)findViewById(R.id.downButton);
        saveCalBtn = (Button)findViewById(R.id.saveCal);
        modeTx = (TextView)findViewById(R.id.modeTx);
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
        //stopAlarm();

        //this.finish();

        //super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        stopAlarm();
        //this.finish();
        //super.onDestroy();
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
                            stopAlarm();
                        }
                    }

                    decibelsTx.setText("" + msg.obj + "dB");
                    double barValue = (100*(double)msg.obj)/(120);
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
                    //stopAlarm();
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

        menu.add(PREFERENCES_GROUP_ID, START_OPTION, 0, "Iniciar").setIcon(
                android.R.drawable.ic_dialog_email);
        menu.add(PREFERENCES_GROUP_ID, STOP_OPTION, 0, "Parar").setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);
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
                    modeTx.setText("");
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
            case STOP_OPTION :
                if (mCalib) break;
                stopAlarm();
                break;
            case START_OPTION :

                if (mCalib){
                    showMessage("Por favor termine la calibración antes de iniciar.");
                }else if (mRunning){

                    showMessage("El monitoreo está en ejecución");
                }else {
                    startAlarm();
                }


        }
        return true;
    }


    /**
     * Iniciar analisis
     */
    public void startAlarm(){

        //mCalib = false;

        modeTx.setText("MONITOREO - ACTIVA");
        modeTx.setTextColor(getResources().getColor(R.color.colorGreen));

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
            modeTx.setText("");
            decibelsTx.setText("0dB");
        }


        //Close app
        /*
        super.onDestroy();
        this.finish();
        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        */
    }

    /**
     * Calibrar analisis
     */
    public void calibrateAction(){

        if (mCalib){
            upCalBtn.setVisibility(View.INVISIBLE);
            downCalBtn.setVisibility(View.INVISIBLE);
            saveCalBtn.setVisibility(View.INVISIBLE);
            mCalib = false;

            decibelsTx.setText("0dB");
            modeTx.setText("");

        }else{

            mEngine = new SoundMeterEngine(mhandle, mContext);
            mEngine.start_engine();

            modeTx.setText("CALIBRACIÓN - ACTIVA");
            modeTx.setTextColor(getResources().getColor(R.color.colorPrimaryDark));

            showMessage("Está en modo calibración\nAjuste los parámetros y guarde los cambios " +
                    "pulsando el botón GUARDAR para continuar");

            upCalBtn.setVisibility(View.VISIBLE);
            downCalBtn.setVisibility(View.VISIBLE);
            saveCalBtn.setVisibility(View.VISIBLE);
            mCalib = true;


        }

    }

    public void upBtnAction(View view){

        mEngine.calibUp();
    }

    public void downBtnAction(View view){
        mEngine.calibDown();
    }

    public void saveCalBtnAction(View view){

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

        mHitMax = Integer.parseInt(prefs.getString("hitsMax", null));
        mThreshold = Integer.parseInt(prefs.getString("threshold", null));

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
