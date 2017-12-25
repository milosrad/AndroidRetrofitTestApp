package mera.com.testapp.api.web;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import mera.com.testapp.api.db.DatabaseHelper;
import mera.com.testapp.api.models.State;
import mera.com.testapp.api.models.States;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.ContentValues.TAG;

public class WebService extends Service
{
    public static final String STATES_UPDATED_ACTION = "states_updated";

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder
    {
        public WebService getService()
        {
            return WebService.this;
        }
    }

    public static Intent createServiceIntent(Context context)
    {
        return new Intent(context, WebService.class);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    public Set<State> getStatesLocal(Context context, String countryFilter, DatabaseHelper.SortType sortType) {

        Set<State> states= null;
        try {
            states = new DBOperation(context,countryFilter,sortType).execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return states;

    }

    public void requestStates(final Context context)
    {

                WebApiManager wm = new WebApiManager();
                Call<States> call = wm.getWebApiInterface().getStates();

                call.clone().enqueue(new Callback<States>() {
                    @Override
                    public void onResponse(Call<States> call, Response<States> response) {

                        try {
                            ArrayList<State> statesArray = new ArrayList<>();
                            States states = response.body();

                            if (states != null) {
                                ArrayList<ArrayList<String>> statesRaw = states.getStates();
                                for (ArrayList<String> stateRaw : statesRaw) {
                                    statesArray.add(State.parse(stateRaw));
                                }
                            }

                            DatabaseHelper helper = DatabaseHelper.getInstance(context);
                            helper.delete();
                            helper.insert(statesArray);

                            sendBroadcast(new Intent(STATES_UPDATED_ACTION));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    }

                    @Override
                    public void onFailure(Call<States> call, Throwable t) {

                        Log.e(TAG, t.toString());

                        Toast.makeText(WebService.this, "There has been an error while loading airplane data.Please try again", Toast.LENGTH_SHORT).show();

                        call.cancel();
                    }
                });








//        new Thread(new Runnable()
//        {
//        @Override
//        public void run() {
//
//            ArrayList<State> statesArray = new ArrayList<>();
//            WebApiManager wm = new WebApiManager();
//            Call<States> call = wm.getWebApiInterface().getStates();
//
//            try
//            {
//                States states = wm.execute(call);
//                if (states != null)
//                {
//                    ArrayList<ArrayList<String>> statesRaw = states.getStates();
//                    for (ArrayList<String> stateRaw : statesRaw)
//                    {
//                        statesArray.add(State.parse(stateRaw));
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//            DatabaseHelper helper = DatabaseHelper.getInstance(context);
//            helper.delete();
//            helper.insert(statesArray);
//
//            sendBroadcast(new Intent(STATES_UPDATED_ACTION));
//        }
//    }).start();

    }


    private class DBOperation extends AsyncTask<Void, Void, Set<State>> {

        Context context;
        String countryFilter;
        DatabaseHelper.SortType sortType;

        public DBOperation(Context context,String countryFilter,DatabaseHelper.SortType sortType){

            this.context=context;
            this.countryFilter=countryFilter;
            this.sortType=sortType;
        }


        @Override
        protected Set<State> doInBackground(Void... voids) {
            Set<State> states;
           states= DatabaseHelper.getInstance(context).query(countryFilter, sortType);

           return states;
        }

        @Override
        protected void onPostExecute(Set<State> states) {
            super.onPostExecute(states);
        }
    }


}
