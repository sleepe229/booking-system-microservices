package com.hotel.init;

import com.hotel.entity.Hotel;
import com.hotel.repo.HotelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final HotelRepository hotelRepository;

    public DataInitializer(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Override
    public void run(String... args) {
        log.info(" Initializing hotel database...");

        if (hotelRepository.count() > 0) {
            log.info("✅ Hotels already exist in database, skipping initialization");
            return;
        }

        List<Hotel> hotels = Arrays.asList(
                // Moscow Hotels
                new Hotel("hotel-moscow-1", "Grand Moscow Hotel", "Moscow", "Red Square, 1", 250.0, true),
                new Hotel("hotel-moscow-2", "Moscow Luxury Suite", "Moscow", "Tverskaya Street, 15", 350.0, true),
                new Hotel("hotel-moscow-3", "Kremlin View Hotel", "Moscow", "Mokhovaya Street, 7", 420.0, true),
                new Hotel("hotel-moscow-4", "Arbat Boutique Hotel", "Moscow", "Arbat Street, 23", 180.0, true),
                new Hotel("hotel-moscow-5", "Moscow City Center", "Moscow", "Neglinnaya Street, 10", 290.0, true),

                // Saint Petersburg Hotels
                new Hotel("hotel-spb-1", "Hermitage Palace Hotel", "Saint Petersburg", "Palace Square, 2", 380.0, true),
                new Hotel("hotel-spb-2", "Neva River Hotel", "Saint Petersburg", "Nevsky Prospect, 45", 320.0, true),
                new Hotel("hotel-spb-3", "Peter's Grand Hotel", "Saint Petersburg", "Bolshaya Morskaya, 12", 450.0, true),
                new Hotel("hotel-spb-4", "Baltic View Suites", "Saint Petersburg", "Vasilyevsky Island, 8", 280.0, true),

                // Sochi Hotels
                new Hotel("hotel-sochi-1", "Black Sea Resort", "Sochi", "Kurortny Prospect, 50", 200.0, true),
                new Hotel("hotel-sochi-2", "Olympic Park Hotel", "Sochi", "Olympic Avenue, 1", 350.0, true),
                new Hotel("hotel-sochi-3", "Sochi Beach Hotel", "Sochi", "Primorskaya Street, 25", 180.0, true),
                new Hotel("hotel-sochi-4", "Mountain View Resort", "Sochi", "Krasnaya Polyana, 5", 420.0, true),

                // Kazan Hotels
                new Hotel("hotel-kazan-1", "Kazan Kremlin Hotel", "Kazan", "Kremlin Street, 1", 220.0, true),
                new Hotel("hotel-kazan-2", "Volga River Hotel", "Kazan", "Bauman Street, 30", 190.0, true),
                new Hotel("hotel-kazan-3", "Tatarstan Grand Hotel", "Kazan", "Pushkin Street, 15", 280.0, true),

                // Vladivostok Hotels
                new Hotel("hotel-vlad-1", "Pacific Ocean Hotel", "Vladivostok", "Svetlanskaya Street, 20", 240.0, true),
                new Hotel("hotel-vlad-2", "Golden Horn Bay Resort", "Vladivostok", "Naberezhnaya, 10", 310.0, true),
                new Hotel("hotel-vlad-3", "Far East Business Hotel", "Vladivostok", "Okeansky Avenue, 45", 270.0, true),

                // Yekaterinburg Hotels
                new Hotel("hotel-ekt-1", "Urals Central Hotel", "Yekaterinburg", "Lenin Avenue, 40", 190.0, true),
                new Hotel("hotel-ekt-2", "Yekaterinburg Plaza", "Yekaterinburg", "Malysheva Street, 5", 250.0, true),

                // Nizhny Novgorod Hotels
                new Hotel("hotel-nn-1", "Volga Grand Hotel", "Nizhny Novgorod", "Bolshaya Pokrovskaya, 12", 210.0, true),
                new Hotel("hotel-nn-2", "Nizhny Novgorod Inn", "Nizhny Novgorod", "Kreml, 1", 180.0, true),

                // Novosibirsk Hotels
                new Hotel("hotel-nsk-1", "Siberian Hotel", "Novosibirsk", "Lenin Square, 5", 170.0, true),
                new Hotel("hotel-nsk-2", "Novosibirsk Business Center", "Novosibirsk", "Krasny Avenue, 28", 220.0, true),

                // Kaliningrad Hotels
                new Hotel("hotel-klg-1", "Baltic Amber Hotel", "Kaliningrad", "Lenin Prospect, 81", 200.0, true),
                new Hotel("hotel-klg-2", "Kaliningrad Harbor Hotel", "Kaliningrad", "Embankment, 3", 240.0, true),

                // Krasnodar Hotels
                new Hotel("hotel-krasnodar-1", "Kuban Hotel", "Krasnodar", "Krasnaya Street, 122", 180.0, true),
                new Hotel("hotel-krasnodar-2", "Krasnodar City Hotel", "Krasnodar", "Severnaya Street, 310", 210.0, true),

                // Международные города для разнообразия
                new Hotel("hotel-dubai-1", "Dubai Luxury Tower", "Dubai", "Sheikh Zayed Road, 1", 500.0, true),
                new Hotel("hotel-paris-1", "Paris Eiffel Hotel", "Paris", "Champ de Mars, 5", 450.0, true),
                new Hotel("hotel-london-1", "London Bridge Hotel", "London", "Tower Bridge Road, 8", 380.0, true),
                new Hotel("hotel-ny-1", "New York Manhattan Plaza", "New York", "5th Avenue, 350", 550.0, true),
                new Hotel("hotel-tokyo-1", "Tokyo Imperial Hotel", "Tokyo", "Chiyoda, 1-1-1", 480.0, true)
        );

        hotelRepository.saveAll(hotels);

        log.info(" Successfully initialized {} hotels in database", hotels.size());
        log.info(" Hotels by city:");

        hotelRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Hotel::getCity,
                        java.util.stream.Collectors.counting()
                ))
                .forEach((city, count) -> log.info("   - {}: {} hotel(s)", city, count));
    }
}
