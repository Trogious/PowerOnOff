package net.swmud.trog.poweronoff;

import android.app.Activity;
import android.content.Intent;
import android.util.Pair;

import java.util.HashMap;
import java.util.Map;

class ResponseRouter {
    private final Map<Long, Pair<String, Class<? extends Activity>>> responseRoutes = new HashMap<>();

    public void addRoute(final long id, final String key, final Class<? extends Activity> clazz) {
        synchronized (responseRoutes) {
            responseRoutes.put(id, new Pair<String, Class<? extends Activity>>(key, clazz));
        }
    }

    public Intent getIntent(final long id, Activity caller, final String msg) {
        Pair<String, Class<? extends Activity>> keyClass;
        synchronized (responseRoutes) {
            keyClass = responseRoutes.remove(id);
        }

        Intent intent = null;
        if (keyClass != null) {
            intent = new Intent(caller, keyClass.second);
            intent.putExtra(keyClass.first, msg);
        }

        return intent;
    }
}
