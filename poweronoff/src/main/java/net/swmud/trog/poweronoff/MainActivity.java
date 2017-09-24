package net.swmud.trog.poweronoff;

import android.os.Bundle;
import android.os.Process;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {
    private final MainActivity self = this;
    private InetAddress broadcast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        final TextView eMac = (TextView) findViewById(R.id.eMac);
        final ImageView iMacValid = (ImageView) findViewById(R.id.imageView);
        setAlertVisibility(iMacValid, eMac.getText().toString());
        eMac.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                setAlertVisibility(iMacValid, editable.toString());
            }
        });

        final TextView textView = (TextView) findViewById(R.id.textView);
        try {
            broadcast = Utils.getBroadcastIpv4();
            textView.setText((broadcast == null) ? "unknown" : broadcast.getHostAddress());
        } catch (IOException e) {
            textView.setText(e.getMessage());
        }

        Button bWol = (Button) findViewById(R.id.bWol);
        bWol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Utils.sendMagicPacket(broadcast, eMac.getText().toString());
                            self.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Snackbar.make(view, "Magic packet sent.", Snackbar.LENGTH_LONG).show();
                                }
                            });
                        } catch (final Exception e) {
                            self.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Snackbar.make(view, e.getMessage(), Snackbar.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }.start();
            }
        });
    }

    private void setAlertVisibility(final ImageView alertImage, final String mac) {
        alertImage.setVisibility(mac.length() == 0 || Utils.isMacValid(mac) ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
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

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            new Thread() {
                @Override
                public void run() {
                    Process.killProcess(Process.myPid());
                }
            }.start();
        }
        super.onDestroy();
    }
}
