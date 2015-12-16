package udea.edu.co.udealarm;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
    /**************************/

    static final int MY_MSG = 1;
    static final int MAXOVER_MSG = 2;
    static final int ERROR_MSG = -1;

    Boolean mMode = false; // false -> fast , true -> slow
    Boolean mCalib = false;
    Boolean mLog = false;
    Boolean mMax = false;
    SoundMeterEngine mEngine = null;
    Context mContext = MainActivity.this;

    /****** Menu context ******/
    static int PREFERENCES_GROUP_ID = 0;
    static final int CALIBRATE_OPTION = 1;
    static final int ABOUT_OPTION = 2;
    static final int STOP_OPTION = 3;
    static final int START_OPTION = 4;

    /***************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        decibelsTx = (TextView)findViewById(R.id.decibelsLB);
        barDB = (ProgressBar)findViewById(R.id.progress_bar);

        upCalBtn = (Button)findViewById(R.id.upButton);
        downCalBtn = (Button)findViewById(R.id.downButton);
        saveCalBtn = (Button)findViewById(R.id.saveCal);
    }

    @Override
    public void onResume() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onResume();
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
        //stopAlarm();
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

        menu.add(PREFERENCES_GROUP_ID, CALIBRATE_OPTION, 0, "Calibrar").setIcon(
                android.R.drawable.ic_menu_revert);
        menu.add(PREFERENCES_GROUP_ID, ABOUT_OPTION, 0, "Instrucciones").setIcon(
                android.R.drawable.ic_menu_help);
        menu.add(PREFERENCES_GROUP_ID, START_OPTION, 0, "Iniciar").setIcon(
                android.R.drawable.ic_dialog_email);
        menu.add(PREFERENCES_GROUP_ID, STOP_OPTION, 0, "Parar").setIcon(
                android.R.drawable.ic_menu_close_clear_cancel);

        return true;
    }

    /**
     * Call back function when an menu option is selected.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CALIBRATE_OPTION :
                calibrateAction();
                break;
            case ABOUT_OPTION :
                about();
                break;
            case STOP_OPTION :
                stopAlarm();
                break;
            case START_OPTION :
                startAlarm();

        }
        return true;
    }


    /**
     * Iniciar analisis
     */
    public void startAlarm(){
        mCalib = false;
        mMax = false;
        mLog = false;
        mMode = false;
        mEngine = new SoundMeterEngine(mhandle, mContext);
        mEngine.start_engine();
    }

    /**
     * Parar analisis
     */
    public void stopAlarm(){
        mEngine.stop_engine();

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
        }else{
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

        calibrateAction();

        mEngine.storeCalibvalue();
        Toast.makeText(mContext, "Calibración guardada.",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Mostrar acerca de
     */
    public void about(){

        Intent i = new Intent(MainActivity.this, AppPreferences.class);
        startActivity(i);

    }
}
