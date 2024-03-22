package com.example.BookTicket.Controller;

//import com.example.BookTicket.Models.SeatsModel;
import com.example.BookTicket.Entity.Stations;
import com.example.BookTicket.Models.SeatsModel;
import com.example.BookTicket.Models.TrainModel;
import com.example.BookTicket.Service.AdminService;
import com.example.BookTicket.validator.TrainValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Controller
public class AdminController {
    @Autowired
    private AdminService adminService;
    private long g_TrainId;

    @Autowired
    private TrainValidator trainValidator;

    @RequestMapping("/saveTrain")
    public String saveTrain()
    {

        return "addTrain";
    }
    @RequestMapping("/addTrain")
    public String addTrain(@ModelAttribute("TrainModel") TrainModel trainModel, Model model, BindingResult bindingResult)
    {
        trainValidator.validate(trainModel,bindingResult);
        System.out.println(bindingResult);
        if(bindingResult.hasErrors())
        {
            System.out.println("bhanu");
            return "addTrain";
        }
        String res= adminService.addTrain(trainModel);
        return "redirect:/DisplayAllTrains";
    }
    @RequestMapping("/removeTrain")
    public String removeTrain(@RequestParam("trainId")Long trainId)
    {

        adminService.removeTrain(trainId);
        return "redirect:/DisplayAllTrains";
    }

    @RequestMapping("/DisplayAllTrains")
    public Object displayAllTrains(Model model)
    {
        List<TrainModel>trainModelList=adminService.displayAllTrains();
        if(!trainModelList.isEmpty())
        {
            model.addAttribute("Train",trainModelList);

            return trainModelList;
        }
        return "no reacords found";
    }
    @RequestMapping("/AddingTicketsToTrain")
    public Object AddingTicketsToTrain(SeatsModel seatsModel)
    {
        seatsModel.setAvailable(true);
        adminService.addingTicketsToTrain(g_TrainId,seatsModel);
        return "redirect:/DisplayadminTrainTickets?trainId="+ g_TrainId;
    }
    @RequestMapping("/AddingStationsToTrain")
    public Object AddingStationsToTrain (Stations stations)
    {

        adminService.addingStationsToTrain(g_TrainId,stations);
        return "redirect:/DisplayIntermediateStations?trainId="+ g_TrainId;
    }


    @RequestMapping("AddingSeats")
    public String AddingTickets(int trainId)
    {
        g_TrainId =trainId;
        return "AddTickents";
    }

    @RequestMapping("/DisplayIntermediateStations")
    public String DisplayIntermediateStations(Model model,Long trainId)
    {
        g_TrainId =trainId;

        Set<Stations>stations=adminService.DisplayIntermediateStations(g_TrainId);
        model.addAttribute("stations",stations);
        return "DisplayStations";

    }
    @RequestMapping("/AddStations")
    public String AddStations()
    {
        return "AddStations";
    }

    @RequestMapping("/DisplayadminTrainTickets")
    public String DisplayTrainTickets(Long trainId, Model model){
        g_TrainId =trainId;
        Set<SeatsModel>seatsModelSet = adminService.displayTrainTickets(trainId);
        model.addAttribute("seatList",seatsModelSet);
        model.addAttribute("trainId",trainId);
        return "displayAdminAllTrainSeats";
    }
}
