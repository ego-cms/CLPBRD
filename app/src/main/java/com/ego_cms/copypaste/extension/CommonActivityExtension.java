package com.ego_cms.copypaste.extension;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

import com.ego_cms.copypaste.ActivityStates;
import com.ego_cms.copypaste.OnBackPressedListener;
import com.ego_cms.copypaste.util.ExtensionBase;
import com.ego_cms.copypaste.util.Lazy;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class CommonActivityExtension extends ExtensionBase<Activity> {

    public CommonActivityExtension(@NotNull Activity instanceExtended) {
        super(instanceExtended);
    }


    public interface ExtensionProvider {

        Handler getMainHandler();


        boolean scheduleTask(@MagicConstant(valuesFromClass = ActivityStates.class) int when,
            @NotNull Runnable task);

        boolean isInState(int state);


        void setOnBackPressedListener(OnBackPressedListener listener);
    }


    private Lazy<Handler> handlerLazy = new Lazy<Handler>(Lazy.LAZY_SYNCHRONIZED_LOCK_FREE) {
        @Override
        protected Handler initialize() {
            return new Handler(Looper.getMainLooper());
        }
    };

    public Handler getMainHandler()
    {
        return handlerLazy.get();
    }


    private final SparseArray<Collection<Runnable>> lingerTasks = new SparseArray<>(0);

    public boolean scheduleTask(@MagicConstant(valuesFromClass = ActivityStates.class) int when, @NotNull Runnable task) {
        if (!isInState(when)) {
            Collection<Runnable> lingerTasksArray = lingerTasks.get(when);

            if (lingerTasksArray == null) {
                lingerTasks.put(when, lingerTasksArray = new ArrayList<>());
            }
            return lingerTasksArray.add(task);
        } else {
            task.run();
        }
        return false;
    }

    public void executeScheduledTasks(int when) {
        if (lingerTasks.size() > 0) {
            Collection<Runnable> lingerTasksArray = lingerTasks.get(when);

            if (lingerTasksArray != null) {
                for (Runnable lingerTask : lingerTasksArray) {
                    getMainHandler().post(lingerTask);
                }
                lingerTasks.remove(when);
            }
        }
    }


    private int stateCurrent = ActivityStates.STATE_INITIAL;

    private void setState(@MagicConstant(valuesFromClass = ActivityStates.class) int state) {
        executeScheduledTasks(stateCurrent = state);
    }

    public boolean isInState(@MagicConstant(valuesFromClass = ActivityStates.class) int state) {
        //noinspection MagicConstant
        return (stateCurrent & state) == state;
    }


    private OnBackPressedListener backPressListener;

    public void setOnBackPressedListener(OnBackPressedListener listener) {
        this.backPressListener = listener;
    }


    public void onCreate(Bundle savedInstanceState) {
        setState(ActivityStates.STATE_CREATED);
    }

    public void onPostCreate(Bundle savedInstanceState) {
        Log.d(((Object) this).getClass().getSimpleName(),
                getInstanceExtended().getResources().getConfiguration().toString());
    }


    public void onStart() {
        setState(ActivityStates.STATE_STARTED);
    }

    public void onResume() {
        setState(ActivityStates.STATE_RESUMED);
    }

    public void onPause() {
        setState(ActivityStates.STATE_PAUSED);
    }

    public void onStop() {
        setState(ActivityStates.STATE_STOPPED);
    }

    public void onDestroy() {
        setState(ActivityStates.STATE_DESTROYED);
    }


    public boolean onBackPressed() {
        return backPressListener != null
                && backPressListener.onBackPressed(getInstanceExtended());
    }
}
