package com.example.BookTicket.Service;

import com.example.BookTicket.Converting.AllConversions;
import com.example.BookTicket.Entity.*;
import com.example.BookTicket.Models.*;
import com.example.BookTicket.Repository.*;
import com.example.BookTicket.ServiceInterface.UserServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class UserService implements UserServiceInterface {
    @Autowired
    private train_Repo trainRepo;

    @Autowired
    private AllConversions convert;

    @Autowired
    private Station_Repo stationRepo;

    @Autowired
    private user_Repo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private booking_Repo bookingRepo;

    @Autowired
    private seat_Repo seatRepo;

    @Autowired
    private bookingDate_Repo bookingDateRepo;

    @Autowired
    private waitingList_Repo waitingListRepo;

    @Override
    public BookingModel bookingTickets(BookingModel bookingModel, Long userId, Long trainId, PriceGenerationModel priceGenerationModel) {
        Train train = trainRepo.findById(trainId).orElse(null);
        if (train == null) {
            return new BookingModel();
        }
        int requestedSeats = bookingModel.getNumerOfSeats();
        Set<Seat> requestedSeatsSet = bookingModel.getSeats();
        //Generation the price
        double price = GenaratePrice(priceGenerationModel, requestedSeatsSet);
        BookingModel newBookingModel = createBookingModel(bookingModel, requestedSeats, price, priceGenerationModel);
        return createBooking(newBookingModel);
    }

    private double GenaratePrice(PriceGenerationModel priceGenerationModel, Set<Seat> requestedSeatsSet) {

        //Generating price based on the kilometers
        int km = priceGenerationModel.getDepartureKm() - priceGenerationModel.getStartKm();
        double price = requestedSeatsSet.stream().mapToDouble(seat -> {
            switch (seat.getTypeOfSeat().toLowerCase()) {
                case "ac":
                    return km * 1.0;
                case "general":
                    return km * 0.4;
                default:
                    return km * 0.7;
            }
        }).sum();
        return price;
    }

    public void displayHighestUserTicketsForTrain()
    {
        Map<User,Integer>heighestuser=new HashMap<>();
        Train train=trainRepo.findById((long)1).orElse(null);

              train.getBookings().forEach(booking -> heighestuser.put(booking.getUser(),heighestuser.getOrDefault(booking.getUser(),0)+booking.getNumerOfSeats()));
              int maxnum=heighestuser.values().stream().max(Integer::compareTo).orElse(0);
              List<User>users=heighestuser.keySet().stream().filter(key->heighestuser.get(key)==maxnum).collect(Collectors.toList());
              users.forEach(user->{
                  System.out.println("User name is: "+user.getUsername()+" Train name is: "+train.getName());
              });

    }
    @Override
    public BookingModel createBookingModel(BookingModel bookingModel, int numberOfSeats, double price, PriceGenerationModel priceGenerationModel) {
        bookingModel.setNumerOfSeats(numberOfSeats);
        bookingModel.setBookingStatus("pending");
        bookingModel.setArrivalStation(priceGenerationModel.getFrom());
        bookingModel.setDestinationStation(priceGenerationModel.getTo());
        bookingModel.setPrice(price);
        Payment payment = new Payment();
        payment.setAmount(price);
        payment.setPaymentStatus("pending");
        bookingModel.setPayment(payment);
        return bookingModel;
    }

    @Override
    public BookingModel createBooking(BookingModel bookingModel) {
        Booking newBooking = convert.bookingModelToBooking(bookingModel);
        Set<Seat> seatSet = newBooking.getSeats();
        seatSet.forEach((seat) -> {
            seat.setBookingSeats(newBooking);
        });
        Booking booking = bookingRepo.save(newBooking);
        return convert.bookingToBookingModel(booking);
    }

    @Override
    public User addUser(UserModel userModel) {
        User user = userRepo.findByUsername(userModel.getUserName());
        if (user == null) {
            User user1 = convert.userModelToUser(userModel);
            return userRepo.save(user1);
        } else {
            return new User();
        }
    }

    @Override
    public BookingModel convertseatNumbersToBookingModel(Set<String> selectedSeatNumbers, Long user_Id, Long Train_Id, PriceGenerationModel priceGenerationModel, LocalDate bookedDate) {
        Set<Seat> seatSet = new HashSet<>();
        Train train=trainRepo.findById(Train_Id).get();
        User user = userRepo.findById(user_Id).orElse(null);
        selectedSeatNumbers.forEach((e) -> {
            long number = Long.parseLong(e);
            Seat seat = seatRepo.findById(number).orElse(null);
            seatSet.add(seat);
        });
        BookingModel bookingModel = new BookingModel();
        bookingModel.setNumerOfSeats(seatSet.size());
        bookingModel.setSeats(seatSet);
        bookingModel.setUser(user);
        bookingModel.setBookedDate(bookedDate);
        LocalDateTime currentDateTime = LocalDateTime.now();
        bookingModel.setBookingTime(currentDateTime);
        bookingModel.setBookingType("normal");
        bookingModel.setTrain(train);
        return bookingTickets(bookingModel, user_Id, Train_Id, priceGenerationModel);

    }

    @Override
    public long checklogin(UserModel userModel) {
        List<User> userList = userRepo.findAll();
        OptionalLong userId = userList.stream()
                .filter(e -> e.getUsername().equals(userModel.getUserName()) && passwordEncoder.matches(userModel.getPassword(), e.getPassword()))
                .mapToLong(User::getId)
                .findFirst();
        return userId.orElse(0);
    }

    @Override
    public List<Booking> findBookingHistory(Long userId) {

        try {
            User user = userRepo.findById(userId).orElse(null);
            assert user != null;
            return user.getBooking();
        } catch (Exception e) {
            return new ArrayList<>();
        }

    }

    @Override
    public BookingModel cancelBooking(Long bookingId) {

        Booking booking = bookingRepo.findById(bookingId).orElse(null);
        //validating time of the booking it should be less than 24 hours
        if (validateTime(booking)) {
            //updating users balance
            UpdatingBalance(booking);
            Set<Seat> seatSetCopy = new HashSet<>(booking.getSeats());
            seatSetCopy.forEach(seat -> {
                seat.getBookingDates().stream()
                        .filter(bookingDate -> bookingDate.getBookingDate().equals(booking.getBookedDate()))
                        .forEach(bookingDate -> {
                            bookingDate.setSeat(null);
                            bookingDateRepo.delete(bookingDate);
                            seat.setSeatStatus("cancelled");
                            changingWaitingListStatus(bookingDate, seat);
                        });
            });
            booking.setBookingStatus("cancelled");
            bookingRepo.save(booking);
            return convert.bookingToBookingModel(booking);
        }
        return new BookingModel();
    }

    @Override
    public void UpdatingBalance(Booking booking) {
        User user = userRepo.findById(booking.getUser().getId()).orElse(null);
        assert user != null;
        user.setBalance(user.getBalance() + booking.getPrice());
        userRepo.save(user);
    }

    @Override
    public Boolean validateTime(Booking booking) {
        if (booking != null) {
            LocalDateTime currentDateTime = LocalDateTime.now();
            long daysDifference = ChronoUnit.DAYS.between(booking.getBookingTime(), currentDateTime);
            return daysDifference < 1;
        }
        return false;
    }


    @Override
    public Set<Seat> displayBookingSeats(Long bookingId) {
        Booking booking = bookingRepo.findById(bookingId).orElse(null);
        assert booking != null;
        try {
            return booking.getSeats(); // Assuming getSeats() returns Set<Seat>
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new HashSet<>(); // Return an empty set if booking is null or if an exception occurs
    }

    @Override
    public BookingModel bookWatingListTickets(int numberOfSeats, String typeOfSeat, Long gUserId, Long gTrainId, LocalDate gBookingDate, PriceGenerationModel priceGenerationModel1) {
        Set<WaitingList> waitingListSet = new HashSet<>();
        Train train = trainRepo.findById(gTrainId).orElse(null);
        User user = userRepo.findById(gUserId).orElse(null);
        IntStream.range(0, numberOfSeats)
                .forEach(i -> CreateWaitingList(gBookingDate, train, typeOfSeat, waitingListSet));

        BookingModel bookingModel = new BookingModel();
        bookingModel.setUser(user);
        bookingModel.setArrivalStation(priceGenerationModel1.getFrom());
        bookingModel.setDestinationStation(priceGenerationModel1.getTo());
        bookingModel.setWaitingLists(waitingListSet);
        bookingModel.setBookedDate(gBookingDate);
        bookingModel.setBookingType("waitingList");
        LocalDateTime currentDateTime = LocalDateTime.now();
        bookingModel.setBookingTime(currentDateTime);


        //converting waitinglist seats to  Seats type  to generate prices

        Set<Seat> seatSet = new HashSet<>();
        waitingListSet.forEach((waitingList) -> {
            Seat seat = new Seat();
            seat.setTypeOfSeat(waitingList.getTypeOfSeat());
            seatSet.add(seat);
        });
        double price = GenaratePrice(priceGenerationModel1, seatSet);
        BookingModel newBookingModel = createBookingModel(bookingModel, numberOfSeats, price, priceGenerationModel1);
        return createWaitingList(newBookingModel);
    }

    @Override
    public void CreateWaitingList(LocalDate gBookingDate, Train train, String typeOfSeat, Set<WaitingList> waitingListSet) {

        WaitingList waitingList = new WaitingList();
        waitingList.setBookedDate(gBookingDate);
        waitingList.setTrain(train);
        waitingList.setTypeOfSeat(typeOfSeat);
        waitingListSet.add(waitingList);
        waitingList.setStatus("notconformed");
        BookingDate bookingDate = new BookingDate();
        bookingDate.setBookingDate(gBookingDate);
        bookingDate.setWaitingList(waitingList);
        waitingListRepo.save(waitingList);
        waitingList.getBookingDates().add(bookingDate);
    }

    @Override
    public BookingModel createWaitingList(BookingModel bookingModel) {
        Booking newBooking = convert.bookingModelToBooking(bookingModel);
        Booking savedBooking = bookingRepo.save(newBooking);

        // Set the savedBooking to each waiting list item
        Set<WaitingList> waitingLists = savedBooking.getWaitingLists();
        waitingLists.forEach(waitingList -> waitingList.setBookingSeats(savedBooking));
        // Save each waiting list item
        waitingListRepo.saveAll(waitingLists);
        // Update the bookingModel with the savedBooking properties

        return convert.bookingToBookingModel(savedBooking);
    }

    @Override
    public void changingWaitingListStatus(BookingDate bookingDate, Seat seat) {
        List<WaitingList> waitingLists = waitingListRepo.findAll();

        waitingLists.stream()
                .filter(waitingList -> waitingList.getBookedDate().equals(bookingDate.getBookingDate())
                        && waitingList.getStatus().equals("notconformed") && waitingList.getTypeOfSeat().equalsIgnoreCase(seat.getTypeOfSeat()) && waitingList.getTrain().equals(seat.getTrain()))
                .sorted(Comparator.comparing(w -> w.getBookingSeats().getBookingTime()))
                .findFirst()
                .ifPresent(waitingList -> {
                    seat.setBookingSeats(waitingList.getBookingSeats());
                    //setting waiting seats to waiting list booking id
                    Booking booking = bookingRepo.findById(waitingList.getBookingSeats().getId()).orElse(null);
                    if (booking != null) {
                        booking.setBookingType("normal");
                        Set<Seat> seatSet = booking.getSeats();
                        seatSet.add(seat);
                        booking.setSeats(seatSet);
                        bookingRepo.save(booking);
                    }
                    //removing train from the waiting list repo
                    Train train = trainRepo.findById(waitingList.getTrain().getId()).orElse(null);
                    Set<WaitingList> waitingListSet = train.getWatitingList();
                    waitingListSet.remove(waitingList);
                    train.setWatitingList(waitingListSet);
                    trainRepo.save(train);
                    //removing booking dates from the waiting list repo
                    List<BookingDate> bookingDateList = waitingList.getBookingDates();
                    bookingDateList.forEach(bookingDate1 -> {
                        bookingDate1.setWaitingList(null);
                        bookingDateRepo.save(bookingDate1);
                    });
                    //removing waiting list
                    removingWaitingList(waitingList);

                    //save seat
                    seatRepo.save(seat);
                    bookingDate.setSeat(seat);
                    bookingDateRepo.save(bookingDate);
                });
    }

    private void removingWaitingList(WaitingList waitingList) {
        waitingList.setBookingDates(new ArrayList<>());
        waitingList.setBookingSeats(null);
        waitingList.setTrain(null);
        waitingListRepo.delete(waitingList);
    }

    @Override
    public List<WaitingList> allWaitingListTickets(Long gUserId) {
        List<WaitingList> waitingLists = waitingListRepo.findAll();
        List<WaitingList> filterWaitingList = new ArrayList<>();
        waitingLists.forEach((waiting) -> {
            if (waiting.getBookingSeats().getUser().getId().equals(gUserId)) {
                filterWaitingList.add(waiting);
            }
            //if waitinglist expires remove the wainting list form the waitinglist repo
            if (waiting.getBookedDate().isBefore(LocalDate.now())) {
                removingWaitingList(waiting);
            }
        });
        return filterWaitingList;
    }

    @Override
    public boolean validateBookingDate(LocalDate BookingDate) {
        LocalDate currentDate = LocalDate.now();
        return !currentDate.isAfter(BookingDate);
    }

    @Override
    public double showBalance(Long gUserId) {
        User user = userRepo.findById(gUserId).orElse(null);
        assert user != null;
        return user.getBalance();
    }

    @Override
    public double recharge(Long gUserId, int balance) {
        User user = userRepo.findById(gUserId).orElse(null);
        double price = (double) balance;
        user.setBalance(user.getBalance() + price);
        userRepo.save(user);
        return user.getBalance();
    }

    @Override
    public Set<SeatsModel> DisplayTrainTickets(Long trainId, LocalDate bookingDate) {
        // Retrieve train from repository
        Train train = trainRepo.findById(trainId).orElse(null);
        if (train == null) {
            return Collections.emptySet();
        }

        // Filter seats that are not booked for the provided date
        Set<SeatsModel> availableSeats = train.getSeats().stream()
                .filter(seat -> seat.getBookingDates().stream()
                        .noneMatch(booking -> booking.getBookingDate().isEqual(bookingDate)))
                .map(convert::seatToSeatModel) // Convert Seat to SeatsModel
                .collect(Collectors.toSet()); // Collect SeatsModel into a Set
        return availableSeats; // Return set of available seats
    }

    @Override
    public List<TrainModel> displayTrainOnLocations(String arrivalLocation, String departureLocation, LocalDate bookingDate) {
        // Get all trains
        List<Train> trainsList = trainRepo.findAll();

        displayHighestUserTicketsForTrain();
        // Filter and process trains

            return trainsList.stream()
                // Converting each Train object to TrainModel
                .map(convert::trainToTrainModel)
                // Filter trains based on stations
                .filter(trainModel -> isValidRoute(trainModel, arrivalLocation, departureLocation))
                // Update availability for each train
                .peek(trainModel -> updateTrainAvailabilitySeats(List.of(trainModel), bookingDate))
                .peek(trainModel -> checkThatkal(trainModel))
                // Collect the filtered trains into a list
                .collect(Collectors.toList());
    }



    private void checkThatkal(TrainModel trainModel) {
        LocalTime trainArivalTime = trainModel.getArrivalTime();
        Duration duration = Duration.ofHours(6);
        LocalTime currentTime = LocalTime.now();
        // Calculate the time 6 hours before the current local date and time
        LocalTime sixHoursBefore = trainArivalTime.minus(duration);
        if (sixHoursBefore.isBefore(currentTime) && (trainModel.getThathakalAddedDate() == null || !trainModel.getThathakalAddedDate().equals(LocalDate.now()))) {
            addingThatkalTickets(trainModel);
        }


    }

    private void addingThatkalTickets(TrainModel trainModel) {
        Train train = convert.trainModelToEntity(trainModel);
        IntStream.range(0, 10).forEach(i -> {

            Seat seat = new Seat();
            seat.setTypeOfSeat("General");
            seat.setAvailable(true);
            train.getSeats().add(seat);
            seat.setTrain(train);
            seatRepo.save(seat);
        });
        train.setThathakalAddedDate(LocalDate.now());
        trainRepo.save(train);
    }

    private boolean isValidRoute(TrainModel trainModel, String arrivalLocation, String departureLocation) {
        //This arrivalFound tag is used to check  arrival station should be found first then after departure station
        AtomicBoolean arrivalFound = new AtomicBoolean(false);

        return trainModel.getStations().stream()
                .sorted(Comparator.comparingInt(Stations::getKm))
                .anyMatch(station -> {
                    if (station.getStationName().equals(arrivalLocation)) {
                        arrivalFound.set(true);
                    } else if (station.getStationName().equals(departureLocation) && arrivalFound.get()) {
                        return true;
                    }
                    return false;
                });
    }

    public void updateTrainAvailabilitySeats(List<TrainModel> filteredTrainsModelList, LocalDate bookingDate) {
        filteredTrainsModelList.forEach(train -> {

            int availableSeats = countAvailableSeats(train, bookingDate);
            train.setAvailableSeats(availableSeats);
        });

    }

    public int countAvailableSeats(TrainModel train, LocalDate bookingDate) {
        return (int) train.getSeats().stream()
                .filter(seat -> isBookingDateAvailable(seat, bookingDate))
                .filter(seat -> isTodayBooking(bookingDate) && isTrainArrivalPending(train) || !isTodayBooking(bookingDate))
                .count();
    }

    private boolean isBookingDateAvailable(Seat seat, LocalDate bookingDate) {
        return seat.getBookingDates().stream()
                .noneMatch(booking -> booking.getBookingDate().isEqual(bookingDate));
    }

    private boolean isTodayBooking(LocalDate bookingDate) {
        return bookingDate.isEqual(LocalDate.now());
    }

    private boolean isTrainArrivalPending(TrainModel train) {
        return train.getArrivalTime().isAfter(LocalTime.now());
    }
}



