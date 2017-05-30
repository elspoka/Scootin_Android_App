package gr.chronosphere.scootin;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;



public class MainActivity extends Activity implements OnClickListener
{

    private Button start;
    private Button pause;
    private Button reset;
    private Chronometer chronograph;
    private long LastStopTime;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rsw();
    }

    public void rsw()
        {
        start = (Button) findViewById(R.id.startbutton);
        start.setOnClickListener(this);
        pause = (Button) findViewById(R.id.pausebutton);
        pause.setOnClickListener(this);
        reset = (Button) findViewById(R.id.resetbutton);
        reset.setOnClickListener(this);
        chronograph = (Chronometer) findViewById(R.id.mainchronometer);
        }

   // @Override
   // public boolean onCreateOptionsMenu(Menu menu)
   // {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
   //     return true;
   // }


    @Override
    public void onClick(View a)
    {
        if (a == start)
        {
            if ( LastStopTime == 0 )
                chronograph.setBase(SystemClock.elapsedRealtime());
                // on resume after pause
            else
            {
                long intervalOnPause = (SystemClock.elapsedRealtime() - LastStopTime);
                chronograph.setBase(chronograph.getBase() + intervalOnPause );
            }

            chronograph.start();
            Toast.makeText(getApplicationContext(),
            "Started!", Toast.LENGTH_SHORT).show();
        }
            else if (a == pause)
            {
                chronograph.stop();
                LastStopTime = SystemClock.elapsedRealtime();
                Toast.makeText(getApplicationContext(),
                        "Paused!", Toast.LENGTH_SHORT).show();
            }
                else if (a == reset)
                {
                 chronograph.setBase(SystemClock.elapsedRealtime());
                 Toast.makeText(getApplicationContext(),
                            "Stopped!", Toast.LENGTH_SHORT).show();
                }
    }
}
