package org.deviceconnect.android.deviceplugin.fabo.setting.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.deviceconnect.android.deviceplugin.fabo.R;
import org.deviceconnect.android.deviceplugin.fabo.param.ArduinoUno;
import org.deviceconnect.android.deviceplugin.fabo.service.virtual.db.ProfileData;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用するピンを1つだけ選択するためのフラグメント.
 */
public class FaBoPinRadioGroupFragment extends FaBoBasePinFragment {

    /**
     * 選択されたPin.
     */
    private ArduinoUno.Pin mSelectedPin;

    /**
     * プロファイルのタイプ.
     * <p>
     * ProfileData.Typeの値.
     * </p>
     */
    private ProfileData mProfileData;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fabo_pin_group, container, false);

        mProfileData = getActivity().getIntent().getParcelableExtra("profile");

        List<ArduinoUno.Pin> pins = getCanUsePinList();

        RadioGroup group = (RadioGroup) view.findViewById(R.id.activity_fabo_pin_layout);
        for (ArduinoUno.Pin pin : pins) {
            group.addView(createRadioButton(inflater, pin),
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        for (ArduinoUno.Pin pin : pins) {
            if (containsPin(pin.getPinNumber())) {
                RadioButton radioButton = (RadioButton) group.findViewWithTag(pin);
                radioButton.setChecked(true);
                mSelectedPin = pin;
            }
        }

        return view;
    }

    /**
     * ピンを選択するためのRadioButtonを作成します.
     * @param inflater インフレータ
     * @param pin ピン
     * @return RadioButtonのインスタンス
     */
    private RadioButton createRadioButton(final LayoutInflater inflater,final ArduinoUno.Pin pin) {
        RadioButton radio = (RadioButton) inflater.inflate(R.layout.item_fabo_radio_button_pin, null, false);
        radio.setText(pin.getPinNames()[1]);
        radio.setTag(pin);
        radio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, final boolean b) {
                if (b) {
                    mSelectedPin = pin;
                }
            }
        });
        return radio;
    }

    /**
     * 指定されたピンがmProfileDataに含まれているか確認します.
     * @param pin ピン
     * @return 含まれている場合はtrue、それ以外はfalse
     */
    private boolean containsPin(final int pin) {
        if (mProfileData == null) {
            return false;
        }
        for (int pinNum : mProfileData.getPinList()) {
            if (pin == pinNum) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Integer> getSelectedPins() {
        List<Integer> pins = new ArrayList<>();
        if (mSelectedPin != null) {
            pins.add(mSelectedPin.getPinNumber());
        }
        return pins;
    }
}

