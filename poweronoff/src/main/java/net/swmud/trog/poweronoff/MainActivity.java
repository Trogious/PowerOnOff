package net.swmud.trog.poweronoff;

import android.os.Bundle;
import android.os.Process;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.swmud.trog.core.BackgroundExecutor;
import net.swmud.trog.json.JsonResponse;
import net.swmud.trog.json.JsonRpc;
import net.swmud.trog.json.PoweroffRequest;
import net.swmud.trog.json.PoweroffResponse;
import net.swmud.trog.net.TcpClient;

import java.io.IOException;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int DEFAULT_PORT = 13013;
    private final MainActivity self = this;
    private final ResponseRouter responseRouter = new ResponseRouter();
    private final BackgroundExecutor backgroundExecutor = new BackgroundExecutor();
    private InetAddress broadcast;
    private TcpClient tcpClient;
    private View snackView;
    private List<String> pendingRequests = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        snackView = findViewById(R.id.textView);
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

        final Button bWol = (Button) findViewById(R.id.bWol);
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

        final Spinner sTime = (Spinner) findViewById(R.id.sTime);
        sTime.setSelection(1);
        final Button bPoweroff = (Button) findViewById(R.id.bPoweroff);
        bPoweroff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backgroundExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        int time = 5;
                        try {
                            time = Integer.parseInt((String)sTime.getSelectedItem());
                        } catch (NumberFormatException e) {
                        }
                        JsonRpc.JsonRequest request = new PoweroffRequest().getJsonRpcRequest(time);
                        synchronized (pendingRequests) {
                            pendingRequests.add(request.toString());
                        }
                        startTcpClient();
                    }
                });
            }
        });

    }

    private void startTcpClient() {
        if (tcpClient != null) {
            tcpClient.finish();
            tcpClient = null;
        }
        if (tcpClient == null || !tcpClient.isRunning()) {
            final TextView eIp = (TextView) findViewById(R.id.eIp);
            final TextView ePort = (TextView) findViewById(R.id.ePort);
            int port = DEFAULT_PORT;
            try {
                port = Integer.parseInt(ePort.getText().toString());
            } catch (NumberFormatException e) {
            }
            tcpClient = new TcpClient(eIp.getText().toString(), port, false, null,
                    new TcpClient.Listener<String>() {
                        @Override
                        public void onMessage(final String msg) {
                            JsonResponse response = null;
                            try {
                                response = new Gson().fromJson(msg, JsonResponse.class);
                            } catch (final JsonSyntaxException e) {
                                self.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Snackbar.make(snackView, getString(R.string.parsing_jsonrpc_response_failed, e.getLocalizedMessage()), Snackbar.LENGTH_LONG).show();
                                    }
                                });
                            }

                            if (response != null) {
                                final PoweroffResponse poffResponse = new Gson().fromJson(msg, PoweroffResponse.class);
                                self.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Snackbar.make(snackView, poffResponse.result.message, Snackbar.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    },
                    new TcpClient.Listener<String>() {
                        @Override
                        public void onMessage(final String msg) {
                            self.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Snackbar.make(snackView, msg, Snackbar.LENGTH_LONG).show();
                                }
                            });
                        }
                    },
                    new TcpClient.ConnectedListener() {
                        @Override
                        public void onConnected() {
                            synchronized (pendingRequests) {
                                while (pendingRequests.size() > 0) {
                                    String request = pendingRequests.remove(0);
                                    tcpClient.sendMessage(request);
                                }
                            }
                        }
                    });
            backgroundExecutor.execute(tcpClient);
        }
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
