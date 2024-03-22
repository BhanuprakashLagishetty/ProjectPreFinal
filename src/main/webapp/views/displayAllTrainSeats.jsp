<%@ page import="java.util.Set" %>
<%@ page import="com.example.BookTicket.Models.SeatsModel" %>
<%@ page import="com.example.BookTicket.Entity.Seat" %>

<!DOCTYPE html>
<html>
<head>
    <title>Train List</title>
      <link rel="stylesheet" type="text/css" href="style.css">
    <style>
        body {
            font-family: Arial, sans-serif;
        }
        h1 {
            color: #333;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th, td {
            border: 1px solid #ddd;
            padding: 8px;
            text-align: left;
        }
        th {
            background-color: #f2f2f2;
            color: #333;
        }
        tr:nth-child(even) {
            background-color: #f9f9f9;
        }
        tr:hover {
            background-color: #f2f2f2;
        }
        .btn-edit, .btn-delete {
            padding: 6px 10px;
            background-color: #007bff;
            color: #fff;
            border: none;
            border-radius: 3px;
            cursor: pointer;
        }
        .btn-delete {
            background-color: #dc3545;
        }
        .btn-edit:hover, .btn-delete:hover {
            background-color: #0056b3;
        }
        .add-btn {
            height: 60px;
            width: 200px;
            background-color: #4CAF50;
            border: none;
            color: white;
            text-align: center;
            text-decoration: none;
            display: inline-block;
            font-size: 16px;
            cursor: pointer;
            border-radius: 5px;
            display: flex;
            text-align: center;
            align-items: center;
            justify-content: center;
        }
        .add-btn:hover {
            background-color: #45a049;
        }
        .form{
            max-width: 627px;
        }
    </style>
</head>
<body>
<h1>${res}</h1>
<h1>Train List</h1>
<div style="display:flex">

    <a href="/" class="add-btn" style="margin-left:10px">GO to index</a>
</div>
<form action="bookingTickets" method="post" style="max-width:600px">
    <table>
        <thead>
            <tr>
                <th>SeatNumber</th>
                <th>AvailabilityOfSeat</th>
                <th>Type of Seat</th>
                <th>Select</th> <!-- Add a new column for checkboxes -->
            </tr>
        </thead>
        <tbody>
            <%
                Set<SeatsModel> seatSet = (Set<SeatsModel>) request.getAttribute("seatList");
                if (seatSet != null && !seatSet.isEmpty()) {
                    for (SeatsModel seat : seatSet) {
            %>
            <tr>
                <td><%= seat.getSeatNumber() %></td>
                <td><%= seat.isAvailable() %></td>
                <td><%= seat.getTypeOfSeat() %></td>
                <td><input type="checkbox" name="selectedSeats" value="<%= seat.getSeatNumber() %>"></td> <!-- Checkbox for selecting the seat -->
            </tr>
            <%
                    }
                } else {
            %>
            <tr>
                <td colspan="4">No Tickets Are Available </td>
            <form:form action="waitingListTickets"  style="max-width:600px">
                <label for="numberOfSeats">Enter No of Seats</label>
                <input type="number" id="numberOfSeats" name="numberOfSeats">
                <label for="typeOfSeat">Select Type of Seat:</label>
                <form:select id="typeOfSeat" path="typeOfSeat">
                    <form:option value="Ac">AC</form:option>
                    <form:option value="General">General</form:option>
                    <form:option value="Sleeper">Sleeper</form:option>
                </form:select><br>
                <input type="submit" value="Submit">
            </form:form>

            </tr>
            <%
                }
            %>
        </tbody>
    </table>
        <input type="hidden" name="bookingDate" value="<%= request.getAttribute("bookingDate") %>">
        <input type="submit" value="Book Selected Seats">
</form>
</body>
</html>
