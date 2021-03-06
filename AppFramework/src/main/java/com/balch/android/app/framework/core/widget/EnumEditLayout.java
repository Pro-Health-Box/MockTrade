/*
 * Author: Balch
 * Created: 9/4/14 12:26 AM
 *
 * This file is part of MockTrade.
 *
 * MockTrade is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MockTrade is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MockTrade.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2014
 */

package com.balch.android.app.framework.core.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.balch.android.app.framework.core.MetadataUtils;
import com.balch.android.app.framework.R;
import com.balch.android.app.framework.core.ColumnDescriptor;
import com.balch.android.app.framework.core.EditState;
import com.balch.android.app.framework.core.ValidatorException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnumEditLayout extends LinearLayout implements EditLayout {
    private static final String TAG = EnumEditLayout.class.getSimpleName();

    protected TextView label;
    protected Spinner value;

    protected ColumnDescriptor descriptor;
    protected EditLayoutListener editLayoutListener;
    protected ControlMapper controlMapper;

    protected List<Object> enumValues;

    public EnumEditLayout(Context context) {
        super(context);
        initialize();
    }

    public EnumEditLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public EnumEditLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    protected void initialize() {
        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setOrientation(VERTICAL);
        int padding = getResources().getDimensionPixelSize(R.dimen.edit_control_padding);
        setPadding(0, padding, 0, padding);

        inflate(getContext(), R.layout.edit_control_enum, this);
        this.label = (TextView) findViewById(R.id.enum_edit_control_label);
        this.value = (Spinner) findViewById(R.id.enum_edit_control_value);
    }

    @Override
    public void bind(final ColumnDescriptor descriptor) {
        this.descriptor = descriptor;
        this.label.setText(descriptor.getLabelResId());

        this.value.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (editLayoutListener != null) {
                    try {
                        editLayoutListener.onChanged(descriptor, getValue(), false);
                    } catch (ValidatorException e) {
                        Log.e(TAG, "Error changing value", e);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        boolean enabled = (descriptor.getState() == EditState.CHANGEABLE);
        try {
            Object obj = descriptor.getField().get(descriptor.getItem());
            List<String> displayValues = new ArrayList<>();
            Object[] enumValues =  ((Enum)obj).getDeclaringClass().getEnumConstants();
            if (obj instanceof MetadataUtils.EnumResource) {
                int resId = ((MetadataUtils.EnumResource) obj).getListResId();
                displayValues.addAll(Arrays.asList(this.getResources().getStringArray(resId)));
            } else {
                for (Object o : enumValues) {
                    displayValues.add(o.toString());
                }
            }

            setOptions(Arrays.asList(enumValues), displayValues, ((Enum)obj).ordinal());

        } catch (IllegalAccessException e) {
            Log.e(TAG, "Error Creating enum", e);
            enabled = false;
        }
        this.value.setEnabled(enabled);
    }

   public void setOptions(List<Object> enumValues, List<String> displayValues, int selectedIndex) {
       this.enumValues = enumValues;
       ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this.getContext(), android.R.layout.simple_spinner_item,
               displayValues);
       dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

       this.value.setAdapter(dataAdapter);
       this.value.setSelection(selectedIndex);
   }

    @Override
    public void setControlMapper(ControlMapper controlMapper) {
        this.controlMapper = controlMapper;
    }

    @Override
    public void validate() throws ValidatorException {
        int position = this.value.getSelectedItemPosition();
        // empty string validation
        if (position < 0) {
            throw new ValidatorException(getResources().getString(R.string.error_empty_string));
        }
    }

    @Override
    public ColumnDescriptor getDescriptor() {
        return this.descriptor;
    }

    @Override
    public Object getValue() {
        try {
            descriptor.getField().get(descriptor.getItem());
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Error Creating enum", e);
            return null;
        }
        return this.enumValues.get(this.value.getSelectedItemPosition());
    }

    @Override
    public void setValue(Object value) {
        for (int x = 0; x < this.enumValues.size(); x++) {
            if (this.enumValues.get(x) == value) {
                this.value.setSelection(x);
                break;
            }
        }
    }

    @Override
    public void setEditControlListener(EditLayoutListener listener) {
        this.editLayoutListener = listener;
    }

}
