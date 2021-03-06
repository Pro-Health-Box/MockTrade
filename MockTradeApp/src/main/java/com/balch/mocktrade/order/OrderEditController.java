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

package com.balch.mocktrade.order;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import com.balch.android.app.framework.core.ColumnDescriptor;
import com.balch.android.app.framework.core.ExternalController;
import com.balch.android.app.framework.core.ValidatorException;
import com.balch.android.app.framework.core.widget.ControlMap;
import com.balch.android.app.framework.core.widget.EditLayout;
import com.balch.android.app.framework.core.widget.EnumEditLayout;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.TradeModelProvider;
import com.balch.mocktrade.finance.FinanceModel;

import java.util.ArrayList;
import java.util.List;

public class OrderEditController implements ExternalController<Order>, Parcelable {
    public OrderEditController() {
    }

    @Override
    public void onChanged(Context context, ColumnDescriptor descriptor, Object value, ControlMap controlMap) throws ValidatorException {
        if (descriptor.getField().getName().equals(Order.FLD_STRATEGY)) {
            onChangeStrategy((Order.OrderStrategy) value, controlMap);
        } else if (descriptor.getField().getName().equals(Order.FLD_ACTION)) {
            onChangeAction(context, (Order.OrderAction) value, controlMap);
        }
    }

    @Override
    public void validate(Context context, Order order, ControlMap controlMap) throws ValidatorException {
        StockSymbolLayout symbolControl = controlMap.get(Order.FLD_SYMBOL);
        Money price = symbolControl.getPrice();
        if (order.getStrategy() == Order.OrderStrategy.MANUAL) {
            price = order.getLimitPrice();
        }
        Money cost = Money.multiply(price, order.getQuantity());

        boolean hasAvailableFunds = ((order.getAction() == Order.OrderAction.SELL) ||
                (cost.getDollars() <= order.getAccount().getAvailableFunds().getDollars()));

        QuantityPriceLayout quantityControl = controlMap.get(Order.FLD_QUANTITY);
        quantityControl.setCost(cost, hasAvailableFunds);

        if (!hasAvailableFunds) {
            throw new ValidatorException(quantityControl.getContext().getString(R.string.quantity_edit_error_insufficient_funds));
        } else if ((cost.getDollars() == 0.0) && (order.getStrategy() != Order.OrderStrategy.MANUAL)) {
            throw new ValidatorException(quantityControl.getContext().getString(R.string.quantity_edit_error_invalid_amount));
        }
    }

    @Override
    public void initialize(Context context, Order order, ControlMap controlMap) {

        boolean controlEnabled = (order.getAction() == Order.OrderAction.BUY);

        TradeModelProvider modelProvider = ((TradeModelProvider)context.getApplicationContext());
        FinanceModel financeModel = modelProvider.getFinanceModel();

        QuantityPriceLayout quantityControl = controlMap.get(Order.FLD_QUANTITY);
        quantityControl.setOrderInfo(order);
        quantityControl.setMarketIsOpen(financeModel.isMarketOpen());
        quantityControl.setAccountInfo(order.getAccount());
        quantityControl.setEnabled(controlEnabled);

        EditLayout control = controlMap.get(Order.FLD_SYMBOL);
        control.setEnabled(controlEnabled);
    }

    private void showControl(EditLayout control, boolean visible) {
        if (control != null) {
            if (control instanceof ViewGroup) {
                ((ViewGroup) control).setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void onChangeStrategy(Order.OrderStrategy strategy, ControlMap controlMap) {
        boolean showLimitPrice = false;
        boolean showStopPrice = false;
        boolean showStopPercent = false;

        switch (strategy) {
            case LIMIT:
            case MANUAL:
                showLimitPrice = true;
                break;
            case STOP_LOSS:
                showStopPrice = true;
                break;
            case TRAILING_STOP_AMOUNT_CHANGE:
                showStopPrice = true;
                break;
            case TRAILING_STOP_PERCENT_CHANGE:
                showStopPercent = true;
                break;
        }

        showControl(controlMap.get(Order.FLD_LIMIT_PRICE), showLimitPrice);
        showControl(controlMap.get(Order.FLD_STOP_PERCENT), showStopPercent);
        showControl(controlMap.get(Order.FLD_STOP_PRICE), showStopPrice);
    }

    private void onChangeAction(Context context, Order.OrderAction action, ControlMap controlMap) {
        EditLayout control = controlMap.get(Order.FLD_STRATEGY);
        if (control instanceof EnumEditLayout) {
            int selectionIndex = 0;
            Order.OrderStrategy strategy = (Order.OrderStrategy) control.getValue();
            List<Object> enumValues = new ArrayList<>();
            List<String> displayValues = new ArrayList<>();

            String [] masterDisplayValues = context.getResources().getStringArray(strategy.getListResId());

            for (int x = 0; x < Order.OrderStrategy.values().length; x++) {
                Order.OrderStrategy s = Order.OrderStrategy.values()[x];
                if ( ((action == Order.OrderAction.BUY) && s.isBuySupported()) ||
                     ((action == Order.OrderAction.SELL) && s.isSellSupported())) {
                    if (s == strategy) {
                        selectionIndex = x;
                    }
                    enumValues.add(s);
                    displayValues.add(masterDisplayValues[x]);
                }
            }

            ((EnumEditLayout)control).setOptions(enumValues, displayValues, selectionIndex);
        }
    }

    private OrderEditController(Parcel in) {
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<OrderEditController> CREATOR = new Creator<OrderEditController>() {
        @Override
        public OrderEditController createFromParcel(Parcel in) {
            return new OrderEditController(in);
        }

        @Override
        public OrderEditController[] newArray(int size) {
            return new OrderEditController[size];
        }
    };

}
