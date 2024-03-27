package com.example.BookTicket.validator;

import com.example.BookTicket.Entity.Stations;
import com.example.BookTicket.Models.TrainModel;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class StationsValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return TrainModel.class.equals(clazz);
    }
    public void validate(Object target, Errors errors) {
        Stations stations = (Stations) target;
        if (stations.getStationName().isBlank()) {
            errors.rejectValue("stationName", "stationNotBlank","station name should not be null");
        }
        if(stations.getKm()<0)
        {
            errors.rejectValue("km", "kmBlank","km should not be null");
        }
    }

}
