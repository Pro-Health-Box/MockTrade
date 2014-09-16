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
import android.view.View;
import android.view.ViewGroup;

import com.balch.android.app.framework.bean.BeanColumnDescriptor;
import com.balch.android.app.framework.bean.BeanExternalController;
import com.balch.android.app.framework.bean.BeanValidatorException;
import com.balch.android.app.framework.bean.controls.BeanControlMap;
import com.balch.android.app.framework.bean.controls.BeanEditControl;
import com.balch.android.app.framework.bean.controls.EnumEditControl;
import com.balch.android.app.framework.model.ModelFactory;
import com.balch.android.app.framework.types.Money;
import com.balch.mocktrade.R;
import com.balch.mocktrade.TradeApplication;
import com.balch.mocktrade.finance.FinanceModel;

import java.util.ArrayList;
import java.util.List;

public class OrderEditController implements BeanExternalController<Order> {

    @Override
    public void onChanged(Context context, BeanColumnDescriptor descriptor, Object value, BeanControlMap beanControlMap) throws BeanValidatorException {
        if (descriptor.getField().getName().equals(Order.FLD_STRATEGY)) {
            onChangeStrategy((Order.OrderStrategy) value, beanControlMap);
        } else if (descriptor.getField().getName().equals(Order.FLD_ACTION)) {
            onChangeAction(context, (Order.OrderAction) value, beanControlMap);
        }
    }

    @Override
    public void validate(Context context, Order order, BeanControlMap beanControlMap) throws BeanValidatorException {
        StockSymbolControl symbolControl = beanControlMap.get(Order.FLD_SYMBOL);
        Money price = symbolControl.getPrice();
        if (order.getStrategy() == Order.OrderStrategy.MANUAL) {
            price = order.getLimitPrice();
        }
        Money cost = Money.multiply(price, order.getQuantity());

        boolean hasAvailableFunds = ((order.getAction() == Order.OrderAction.SELL) ||
                (cost.getDollars() <= order.getAccount().getAvailableFunds().getDollars()));

        QuantityPriceControl quantityControl = beanControlMap.get(Order.FLD_QUANTITY);
        quantityControl.setCost(cost, hasAvailableFunds);

        if (!hasAvailableFunds) {
            throw new BeanValidatorException(quantityControl.getContext().getString(R.string.quantity_edit_error_insufficient_funds));
        } else if ((cost.getDollars() == 0.0) && (order.getStrategy() != Order.OrderStrategy.MANUAL)) {
            throw new BeanValidatorException(quantityControl.getContext().getString(R.string.quantity_edit_error_invalid_amount));
        }
    }

    @Override
    public void initialize(Context context, Order order, BeanControlMap beanControlMap) {

        boolean controlEnabled = (order.getAction() == Order.OrderAction.BUY);

        ModelFactory modelFactory = TradeApplication.getInstance().getModelFactory();
        FinanceModel financeModel = modelFactory.getModel(FinanceModel.class);

        QuantityPriceControl quantityControl = beanControlMap.get(Order.FLD_QUANTITY);
        quantityControl.setOrderInfo(order);
        quantityControl.setMarketIsOpen(financeModel.isMarketOpen());
        quantityControl.setAccountInfo(order.account);
        quantityControl.setEnabled(controlEnabled);

        BeanEditControl control = beanControlMap.get(Order.FLD_SYMBOL);
        control.setEnabled(controlEnabled);
    }

    protected void showControl(BeanEditControl control, boolean visible) {
        if (control != null) {
            if (control instanceof ViewGroup) {
                ((ViewGroup) control).setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }
    }

    protected void onChangeStrategy(Order.OrderStrategy strategy, BeanControlMap beanControlMap) {
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

        showControl(beanControlMap.get(Order.FLD_LIMIT_PRICE), showLimitPrice);
        showControl(beanControlMap.get(Order.FLD_STOP_PERCENT), showStopPercent);
        showControl(beanControlMap.get(Order.FLD_STOP_PRICE), showStopPrice);
    }

    protected void onChangeAction(Context context, Order.OrderAction action, BeanControlMap beanControlMap) {
        BeanEditControl control = beanControlMap.get(Order.FLD_STRATEGY);
        if (control instanceof EnumEditControl) {
            int selectionIndex = 0;
            Order.OrderStrategy strategy = (Order.OrderStrategy) control.getValue();
            List<Object> enumValues = new ArrayList<Object>();
            List<String> displayValues = new ArrayList<String>();

            String [] masterDisplayValues = context.getResources().getStringArray(strategy.getListResId());

            for (int x = 0; x < Order.OrderStrategy.values().length; x++) {
                Order.OrderStrategy s = Order.OrderStrategy.values()[x];
                if ( ((action == Order.OrderAction.BUY) && s.isBuySuported()) ||
                     ((action == Order.OrderAction.SELL) && s.isSellSuported())) {
                    if (s == strategy) {
                        selectionIndex = x;
                    }
                    enumValues.add(s);
                    displayValues.add(masterDisplayValues[x]);
                }
            }

            ((EnumEditControl)control).setOptions(enumValues, displayValues, selectionIndex);
        }

    }
}
