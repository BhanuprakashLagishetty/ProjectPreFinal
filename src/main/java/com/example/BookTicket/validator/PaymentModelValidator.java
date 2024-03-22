package com.example.BookTicket.validator;

import com.example.BookTicket.Models.PaymentModel;
import com.example.BookTicket.Models.UserModel;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class PaymentModelValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return PaymentModel.class.equals(clazz);
    }
    public void validate(Object target, Errors errors) {
        PaymentModel paymentModel = (PaymentModel) target;
        if (paymentModel.getAmount()<=0) {
            errors.rejectValue("amount", "amount","amount should not be null");
        }
    }
}
