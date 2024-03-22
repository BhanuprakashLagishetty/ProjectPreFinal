package com.example.BookTicket.Service;

import com.example.BookTicket.Converting.AllConversions;
import com.example.BookTicket.Entity.*;
//import com.example.BookTicket.Models.SeatsModel;
import com.example.BookTicket.Models.*;
import com.example.BookTicket.Repository.Station_Repo;
import com.example.BookTicket.Repository.admin_Repo;
import com.example.BookTicket.Repository.seat_Repo;
import com.example.BookTicket.Repository.train_Repo;
import com.example.BookTicket.ServiceInterface.AdminInterface;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class AdminService implements AdminInterface {
    @Autowired
    private train_Repo trainRepo;
    @Autowired
    private admin_Repo adminRepo;
    @Autowired
    private AllConversions convert;

    @Autowired
    private seat_Repo seatRepo;

    @Autowired
    private Station_Repo stationRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Override
    public String addTrain(TrainModel trainModel)
    {
        Train train=convert.trainModelToEntity(trainModel);
        train.setAvailableSeats(30);
        List<TrainRuns>trainRuns=train.getDays();
        trainRuns.stream().forEach((e)->{
            e.setTrain(train);
        });
        addingFixedSeatsToTrain(trainRepo.save(train));
        trainRepo.save(train);
        return "Succesfully Added train";

    }

    private void addingFixedSeatsToTrain(Train train) {
        IntStream.range(0,10).forEach(i->{

            Seat seat=new Seat();
            seat.setTypeOfSeat("Ac");
            seat.setAvailable(true);
            train.getSeats().add(seat);
            seat.setTrain(train);
            seatRepo.save(seat);
        });
        IntStream.range(0,10).forEach(i->{

            Seat seat=new Seat();
            seat.setTypeOfSeat("General");
            seat.setAvailable(true);
            train.getSeats().add(seat);
            seat.setTrain(train);
            seatRepo.save(seat);
        });
        IntStream.range(0,10).forEach(i->{

            Seat seat=new Seat();
            seat.setTypeOfSeat("Sleeper");
            seat.setAvailable(true);
            train.getSeats().add(seat);
            seat.setTrain(train);
            seatRepo.save(seat);
        });
    }

    @Override
    public void removeTrain(Long id)
    {
        try{
        Train train=trainRepo.findById(id).get();
            Set<User> users=train.getTrain_Passangers();
            for (User user1:users)
            {
                Set<Train>trainList=user1.getTrain();
                if(trainList.contains(train))
                {
                    trainList.remove(train);
                    user1.setTrain(trainList);
                }
            }
            Set<Seat>seatSet=train.getSeats();
            seatSet.forEach((seat)->{
                seat.setTrain(null);
                seatRepo.delete(seat);
            });

            Set<Stations>stations=train.getStations();
            stations.forEach((singStation)->{
                singStation.setTrain(null);
                stationRepo.delete(singStation);

            });
            train.setSeats(null);
            train.setStations(null);
            train.setWatitingList(null);
            train.setTrain_Passangers(new HashSet<>());
            trainRepo.delete(train);
        }
        catch (Exception e){
            System.out.println(e);
        }


    }
    @Override
    public List<TrainModel> displayAllTrains()
    {
        List<Train>trainList=trainRepo.findAll();
        List<TrainModel>trainModelList=new ArrayList<>();
        trainList.stream().forEach((e)->{
           TrainModel trainModel= convert.trainToTrainModel(e);
           trainModelList.add(trainModel);
        });
        return trainModelList;
    }
    @Override
    public Train addingTicketsToTrain(Long trainId, SeatsModel seatsModel) {
        Train train = trainRepo.findById(trainId).orElse(null);

        Seat seat=convert.seatModelToSeat(seatsModel);
        if (train == null) {
            return null; // Handle train not found scenario
        }
        Set<Seat> existingSeats = train.getSeats();
        seat.setTrain(train);
        existingSeats.add(seat);
        int count = (int) existingSeats.stream()
                .peek(seat1 -> seat1.setTrain(train)) // Update the train reference for each seat
                .filter(Seat::isAvailable)
                .count();
        train.setAvailableSeats(count);
        trainRepo.save(train);
        return train;
    }
    @Override
    public String addingStationsToTrain(Long trainId, Stations stations)
    {
        Train train=trainRepo.findById(trainId).orElse(null);
       try
        {
            Set<Stations> stations1=train.getStations();
            stations.setTrain(train);
            stations1.add(stations);
            train.setStations(stations1);
            trainRepo.save(train);
            return "Station added succesfully";
        }
       catch (Exception e){
            return "Train id is not present";
        }
    }

    @Override
    public Set<SeatsModel> displayTrainTickets(Long trainId) {
        return trainRepo.findById(trainId)
                .map(train -> train.getSeats().stream()
                        .filter(seat -> seat.getBookingDates().isEmpty())
                        .map(convert::seatToSeatModel)
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet());
    }

    @Override

    public Set<Stations> DisplayIntermediateStations(Long trainId) {
        Train train=trainRepo.findById(trainId).orElse(null);
        if(train!=null)
        {
            return train.getStations();
        }
        else{
            return new HashSet<>();
        }

    }

    @Override
    public boolean checkAdminLogin(AdminModel adminModel) {
        System.out.println("admin");
        if(adminRepo.existsByUsername(adminModel.getUsername())) {
            Admin admin = adminRepo.findByUsername(adminModel.getUsername());
            return passwordEncoder.matches(adminModel.getPassword(), admin.getPassword());
        }
        return false;
    }

    //used to search for killo meters
    @Override
    public PriceGenerationModel findKilometers(String arrivalStation, String departureStation, List<TrainModel> trainModelList) {
        return trainModelList.stream()
                .findFirst() // Get the first train model from the list
                .map(train -> {
                    Set<Stations> stations = train.getStations();
                    PriceGenerationModel priceGenerationModel = new PriceGenerationModel();
                    stations.forEach(stations1 -> {
                        if (stations1.getStationName().equals(arrivalStation)) {
                            priceGenerationModel.setFrom(arrivalStation);
                            priceGenerationModel.setStartKm(stations1.getKm());
                        }
                        if (stations1.getStationName().equals(departureStation)) {
                            priceGenerationModel.setTo(departureStation);
                            priceGenerationModel.setDepartureKm(stations1.getKm());
                        }
                    });
                    return priceGenerationModel;
                })
                .orElse(new PriceGenerationModel()); // Handle case where trainModelList is empty
    }


}
