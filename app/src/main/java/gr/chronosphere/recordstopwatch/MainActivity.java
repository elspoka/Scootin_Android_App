package gr.chronosphere.recordstopwatch;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Chronometer;



public class MainActivity extends Activity implements OnClickListener {

    private Button start;
    private Button pause;
    private Button reset;
    private Chronometer chronograph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public void onClick(View a) {
        if (a == start) {
            chronograph.start();
        } else if (a == pause) {
            chronograph.stop();
        } else if (a == reset) {
            chronograph.setBase(SystemClock.elapsedRealtime());
        }
    }
}



   /* @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
*/