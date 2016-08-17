package no.iegget.bluetherm.ui;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import no.iegget.bluetherm.BluetoothService;
import no.iegget.bluetherm.MainActivity;
import no.iegget.bluetherm.R;

public class AlarmDialogFragment extends DialogFragment {

    private final String TAG = getClass().getSimpleName();

    private static final int ANIMATION_DURATION_MS = 500;

    private Animation upAnimation;
    private Animation downAnimation;
    private ValueAnimator upColorChange;
    private ValueAnimator downColorChange;
    private View view;
    private TextInputLayout temperatureLayout;
    private EditText temperature;
    private LinearLayout directionLayout;
    private ImageView directionArrow;
    private TextView directionText;
    private int direction;
    private AlarmDialogListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.dialog_alarm, null);
        bindViews();
        setupPreferences();
        setupAnimations();
        setupDirection();
        return getAlarmDialog();
    }

    private void bindViews() {
        temperatureLayout = (TextInputLayout) view.findViewById(R.id.temperature_layout);
        temperature = (EditText) view.findViewById(R.id.temperature);
        directionLayout = (LinearLayout) view.findViewById(R.id.directionLayout);
        directionArrow = (ImageView) view.findViewById(R.id.directionArrow);
        directionText = (TextView) view.findViewById(R.id.directionText);
    }

    private Dialog getAlarmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setPositiveButton(R.string.set, null)
                .setNeutralButton(R.string.cancel, null)
                .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onDismissAlarm();
                    }
                });
        final AlertDialog alarmDialog = builder.create();
        alarmDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positiveButton = alarmDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            listener.onSetAlarm(
                                    Float.valueOf(temperature.getText().toString()),
                                    direction
                            );
                            alarmDialog.dismiss();
                        } catch (NumberFormatException e) {
                            Log.e(TAG, e.getMessage());
                            temperatureLayout.setError(getString(R.string.temperature_error));
                        }
                    }
                });
            }
        });
        alarmDialog.setCanceledOnTouchOutside(false);
        return alarmDialog;
    }

    private void setupAnimations() {
        if (direction == BluetoothService.DIRECTION_DESCENDING) {
            upAnimation = AnimationUtils.loadAnimation(
                    getActivity(),
                    R.anim.rotate_up
            );
            downAnimation = AnimationUtils.loadAnimation(
                    getActivity(),
                    R.anim.rotate_down
            );
        } else {
            upAnimation = AnimationUtils.loadAnimation(
                    getActivity(),
                    R.anim.rotate_down
            );
            downAnimation = AnimationUtils.loadAnimation(
                    getActivity(),
                    R.anim.rotate_up
            );
        }
        upAnimation.setFillAfter(true);
        downAnimation.setFillAfter(true);
        upAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                directionText.setText(R.string.increasing);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        downAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                directionText.setText(R.string.decreasing);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        ValueAnimator.AnimatorUpdateListener updateListener =
                new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                directionArrow.setImageTintList(
                        ColorStateList.valueOf((Integer) animation.getAnimatedValue())
                );
            }
        };
        downColorChange = new ValueAnimator();
        downColorChange.setIntValues(Color.RED, Color.BLUE);
        downColorChange.setEvaluator(new ArgbEvaluator());
        downColorChange.setDuration(ANIMATION_DURATION_MS);
        downColorChange.addUpdateListener(updateListener);

        upColorChange = new ValueAnimator();
        upColorChange.setIntValues(Color.BLUE, Color.RED);
        upColorChange.setEvaluator(new ArgbEvaluator());
        upColorChange.setDuration(ANIMATION_DURATION_MS);
        upColorChange.addUpdateListener(updateListener);
    }

    private void setupDirection() {
        directionLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (direction) {
                    case BluetoothService.DIRECTION_ASCENDING:
                        downColorChange.start();
                        directionArrow.startAnimation(downAnimation);
                        direction = BluetoothService.DIRECTION_DESCENDING;
                        break;
                    case BluetoothService.DIRECTION_DESCENDING:
                        upColorChange.start();
                        directionArrow.startAnimation(upAnimation);
                        direction = BluetoothService.DIRECTION_ASCENDING;
                        break;
                }
            }
        });
    }

    private void setupPreferences() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                MainActivity.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
        temperature.setText(String.valueOf(
                sharedPreferences.getFloat(BluetoothService.DESIRED_TEMPERATURE, 50F)
        ));
        direction = sharedPreferences.getInt(
                BluetoothService.DIRECTION,
                BluetoothService.DIRECTION_ASCENDING
        );
        if (direction == BluetoothService.DIRECTION_ASCENDING) {
            directionArrow.setImageResource(R.drawable.ic_arrow_upward_red_48dp);
            directionText.setText(R.string.increasing);
        } else if (direction == BluetoothService.DIRECTION_DESCENDING) {
            directionArrow.setImageResource(R.drawable.ic_arrow_downward_blue_48dp);
            directionText.setText(R.string.decreasing);
        }
    }

    public void setListener(AlarmDialogListener listener) {
        this.listener = listener;
    }

    public interface AlarmDialogListener {
        void onDismissAlarm();
        void onSetAlarm(float temperature, int direction);
    }
}
