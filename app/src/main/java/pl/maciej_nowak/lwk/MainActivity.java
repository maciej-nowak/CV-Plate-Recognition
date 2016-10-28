package pl.maciej_nowak.lwk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button lwk2 = (Button) findViewById(R.id.lwk2);
        Button lwk3 = (Button) findViewById(R.id.lwk3);
        Button lwk4 = (Button) findViewById(R.id.lwk4);
        Button lwk5 = (Button) findViewById(R.id.lwk5);

        lwk2.setOnClickListener(onLWKClick(this, LWK2.class));
        lwk3.setOnClickListener(onLWKClick(this, LWK3.class));
        lwk4.setOnClickListener(onLWKClick(this, LWK4.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private View.OnClickListener onLWKClick(final Context context, final Class cl) {
        return (new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, cl);
                startActivity(intent);
            }
        });
    }

}
