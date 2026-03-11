-- =============================================================
-- CommunityBoard Seed Data
-- 03_seed_data.sql
-- Generates realistic sample data for testing:
--   - 20 users
--   - 60 posts distributed across 4 categories
--   - 200+ comments across posts
--
-- Schema alignment:
--   posts.category  → STRING ENUM: 'NEWS','EVENT','DISCUSSION','ALERT'
--   posts.body      → TEXT column (renamed from content in earlier version)
--   No categories table — category is stored directly on posts
--
-- Safe to re-run: only inserts if users table is empty
-- =============================================================

DO $$
BEGIN
IF (SELECT COUNT(*) FROM users) < 5 THEN

    -- ---------------------------------------------------------
    -- SEED USERS (20 users)
    -- Passwords are bcrypt hash of 'Password123!'
    -- ---------------------------------------------------------
    INSERT INTO users (email, name, password, role, created_at) VALUES
        ('john.smith@email.com',         'John Smith',         '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '90 days'),
        ('brooklyn.simmons@email.com',   'Brooklyn Simmons',   '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '85 days'),
        ('kristin.watson@email.com',     'Kristin Watson',     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '80 days'),
        ('courtney.henry@email.com',     'Courtney Henry',     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '78 days'),
        ('leslie.alexander@email.com',   'Leslie Alexander',   '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '75 days'),
        ('dianne.russell@email.com',     'Dianne Russell',     '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '70 days'),
        ('jerome.bell@email.com',        'Jerome Bell',        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '68 days'),
        ('savannah.nguyen@email.com',    'Savannah Nguyen',    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '65 days'),
        ('cameron.williamson@email.com', 'Cameron Williamson', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '60 days'),
        ('esther.howard@email.com',      'Esther Howard',      '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '58 days'),
        ('jacob.jones@email.com',        'Jacob Jones',        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '55 days'),
        ('bessie.cooper@email.com',      'Bessie Cooper',      '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '50 days'),
        ('floyd.miles@email.com',        'Floyd Miles',        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '48 days'),
        ('jenny.wilson@email.com',       'Jenny Wilson',       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '45 days'),
        ('ralph.edwards@email.com',      'Ralph Edwards',      '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '40 days'),
        ('cody.fisher@email.com',        'Cody Fisher',        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '38 days'),
        ('annette.black@email.com',      'Annette Black',      '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '35 days'),
        ('theresa.webb@email.com',       'Theresa Webb',       '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '30 days'),
        ('albert.flores@email.com',      'Albert Flores',      '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'USER',  NOW() - INTERVAL '25 days'),
        ('admin@communityboard.com',     'Admin User',         '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'ADMIN', NOW() - INTERVAL '90 days');


    -- ---------------------------------------------------------
    -- SEED POSTS (60 posts)
    -- category column: direct ENUM string — 'NEWS', 'EVENT', 'DISCUSSION', 'ALERT'
    -- body column:     TEXT content of the post (renamed from content)
    -- ---------------------------------------------------------

    -- NEWS posts (18 posts ~30%)
    INSERT INTO posts (title, body, category, author_id, created_at, updated_at) VALUES
        ('New Community Center Opening Next Month',
         'The long-awaited community center on Maple Street is set to open its doors next month. The facility will include a gym, meeting rooms, and a library.',
         'NEWS', 1, NOW() - INTERVAL '88 days', NOW() - INTERVAL '88 days'),
        ('Road Construction Update: Main Street Closure',
         'Main Street between Oak Ave and Pine Rd will be closed for resurfacing starting Monday. Expect detours via Elm Street for approximately 2 weeks.',
         'NEWS', 2, NOW() - INTERVAL '85 days', NOW() - INTERVAL '85 days'),
        ('Local School Receives State Excellence Award',
         'Jefferson Elementary has been awarded the State Excellence in Education award for the third consecutive year. Principal Adams will host a celebration assembly.',
         'NEWS', 3, NOW() - INTERVAL '80 days', NOW() - INTERVAL '80 days'),
        ('New Bus Route Added to Neighborhood',
         'The city transit authority has announced a new bus route 47 that will serve our neighborhood every 20 minutes during peak hours starting next week.',
         'NEWS', 4, NOW() - INTERVAL '75 days', NOW() - INTERVAL '75 days'),
        ('Community Garden Expansion Approved',
         'The city council has approved the expansion of the Riverside Community Garden. Twenty new plots will be available for residents to apply for starting next spring.',
         'NEWS', 1, NOW() - INTERVAL '70 days', NOW() - INTERVAL '70 days'),
        ('Library Extended Hours Announcement',
         'The neighborhood library will now be open until 9 PM on weekdays and 6 PM on weekends starting from next month. New digital lending services also launching.',
         'NEWS', 5, NOW() - INTERVAL '65 days', NOW() - INTERVAL '65 days'),
        ('Street Lighting Upgrade Project Begins',
         'The city has begun upgrading all street lights on residential roads to energy-efficient LED fixtures. The project is expected to take 6 weeks to complete.',
         'NEWS', 6, NOW() - INTERVAL '60 days', NOW() - INTERVAL '60 days'),
        ('New Recycling Program Launched',
         'The neighborhood is participating in a new enhanced recycling program. Blue bins for electronics and green bins for organics have been distributed to all households.',
         'NEWS', 2, NOW() - INTERVAL '55 days', NOW() - INTERVAL '55 days'),
        ('Neighborhood Watch Program Expands',
         'The neighborhood watch program has expanded to cover the eastern blocks. New coordinators have been appointed and training sessions will be held next Saturday.',
         'NEWS', 7, NOW() - INTERVAL '50 days', NOW() - INTERVAL '50 days'),
        ('Park Renovation Complete',
         'Riverside Park renovation has been completed ahead of schedule. New playground equipment, benches, and walking paths are now open for residents to enjoy.',
         'NEWS', 3, NOW() - INTERVAL '45 days', NOW() - INTERVAL '45 days'),
        ('Local Pharmacy Now Offers Health Screenings',
         'MediCare Pharmacy on Central Ave is now offering free blood pressure and diabetes screenings every Tuesday from 10 AM to 2 PM.',
         'NEWS', 8, NOW() - INTERVAL '40 days', NOW() - INTERVAL '40 days'),
        ('Speed Limit Reduction on School Zone',
         'Following a petition from residents, the city has approved reducing the speed limit from 40 to 25 km/h in the school zone on Birch Street.',
         'NEWS', 1, NOW() - INTERVAL '35 days', NOW() - INTERVAL '35 days'),
        ('New Grocery Store Coming to Neighborhood',
         'A new FreshMart grocery store has been approved for the corner of Oak and 5th. Construction begins in spring with an expected opening before year end.',
         'NEWS', 4, NOW() - INTERVAL '28 days', NOW() - INTERVAL '28 days'),
        ('Annual Report: Neighborhood Crime Down 15%',
         'The annual safety report shows neighborhood crime has decreased by 15% compared to last year. Police credit community cooperation and the watch program.',
         'NEWS', 9, NOW() - INTERVAL '21 days', NOW() - INTERVAL '21 days'),
        ('Community Solar Project Applications Open',
         'Residents can now apply to join the community solar project. Participants will receive a 20% reduction on their electricity bills. Applications close end of month.',
         'NEWS', 5, NOW() - INTERVAL '14 days', NOW() - INTERVAL '14 days'),
        ('Pothole Repair Schedule Released',
         'The public works department has released the Q1 pothole repair schedule. All identified potholes on residential streets will be addressed within 30 days.',
         'NEWS', 2, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
        ('New Dog Park Opens on Elm Street',
         'The new off-leash dog park on Elm Street is now officially open. The park features separate areas for large and small dogs, water stations, and seating.',
         'NEWS', 6, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
        ('Fiber Internet Coming to All Streets',
         'The telecom provider has confirmed fiber internet rollout will reach all streets in our neighborhood by end of Q2. Speeds of up to 1Gbps will be available.',
         'NEWS', 1, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days');

    -- EVENT posts (18 posts ~30%)
    INSERT INTO posts (title, body, category, author_id, created_at, updated_at) VALUES
        ('Annual Neighborhood BBQ - Save the Date!',
         'Join us for the annual neighborhood BBQ at Riverside Park on the last Saturday of the month. Bring a dish to share. All residents welcome!',
         'EVENT', 3, NOW() - INTERVAL '87 days', NOW() - INTERVAL '87 days'),
        ('Summer Concert Series Kickoff',
         'The summer concert series kicks off this Friday at 6 PM in Central Park. Featuring local band The Riverside Collective. Free entry for all residents.',
         'EVENT', 10, NOW() - INTERVAL '82 days', NOW() - INTERVAL '82 days'),
        ('Community Cleanup Day - Volunteers Needed',
         'Join us this Saturday from 8 AM to noon for our quarterly neighborhood cleanup. Gloves and bags provided. Meet at the park entrance.',
         'EVENT', 4, NOW() - INTERVAL '78 days', NOW() - INTERVAL '78 days'),
        ('Back to School Supply Drive',
         'We are collecting school supplies for underprivileged children in our area. Drop off points at the library, community center, and church until end of August.',
         'EVENT', 11, NOW() - INTERVAL '73 days', NOW() - INTERVAL '73 days'),
        ('Neighborhood Garage Sale Weekend',
         'The annual neighborhood garage sale is happening this weekend. Register your address on the community board to be included in the map. Shoppers welcome!',
         'EVENT', 5, NOW() - INTERVAL '68 days', NOW() - INTERVAL '68 days'),
        ('Fall Festival Planning Meeting',
         'All residents interested in helping organize the fall festival are invited to a planning meeting at the community center on Thursday at 7 PM.',
         'EVENT', 12, NOW() - INTERVAL '63 days', NOW() - INTERVAL '63 days'),
        ('Free Yoga in the Park - Every Sunday',
         'Certified instructor Maria is hosting free yoga sessions every Sunday morning at 8 AM in Riverside Park. Bring your mat. All levels welcome.',
         'EVENT', 7, NOW() - INTERVAL '58 days', NOW() - INTERVAL '58 days'),
        ('Kids Halloween Trick or Treat Trail',
         'The children''s Halloween trail will run along Oak Street and Maple Avenue on October 31st from 5 to 8 PM. Participating houses will display pumpkins.',
         'EVENT', 3, NOW() - INTERVAL '53 days', NOW() - INTERVAL '53 days'),
        ('Town Hall Meeting - Budget Discussion',
         'The mayor will host a town hall meeting to discuss the neighborhood improvement budget for next year. Your input matters. Monday 7 PM at City Hall.',
         'EVENT', 8, NOW() - INTERVAL '48 days', NOW() - INTERVAL '48 days'),
        ('Charity 5K Run Registration Open',
         'Registration is now open for the annual charity 5K run benefiting the local food bank. Race day is the first Sunday of next month. Register at the library.',
         'EVENT', 1, NOW() - INTERVAL '43 days', NOW() - INTERVAL '43 days'),
        ('Senior Citizens Luncheon',
         'The monthly senior citizens luncheon will be held at the community center this Friday at noon. Transport assistance available. Please RSVP by Wednesday.',
         'EVENT', 13, NOW() - INTERVAL '38 days', NOW() - INTERVAL '38 days'),
        ('Book Club - New Season Starting',
         'The neighborhood book club is starting a new season. First meeting is next Tuesday at 7 PM at the library. New members welcome. This season''s first book: The Midnight Library.',
         'EVENT', 6, NOW() - INTERVAL '33 days', NOW() - INTERVAL '33 days'),
        ('Community Art Exhibition',
         'Local artists are invited to submit works for the annual community art exhibition at the library gallery. Submissions accepted until the 15th of this month.',
         'EVENT', 9, NOW() - INTERVAL '28 days', NOW() - INTERVAL '28 days'),
        ('Holiday Lights Competition',
         'Get your decorations ready! The annual holiday lights competition runs December 1-20. Register your home and let the community vote for the best display.',
         'EVENT', 2, NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
        ('Neighborhood Movie Night',
         'Outdoor movie night this Saturday at dusk in Riverside Park. Showing: The Lion King. Bring blankets, chairs, and snacks. Free for all residents.',
         'EVENT', 14, NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days'),
        ('Cooking Class: Healthy Meals on a Budget',
         'Free cooking class at the community center next Wednesday at 6 PM. Chef Maria will demonstrate 5 healthy meals under $10. Limited to 20 participants.',
         'EVENT', 4, NOW() - INTERVAL '9 days', NOW() - INTERVAL '9 days'),
        ('Youth Sports Registration Open',
         'Registration for the fall youth sports league is now open for children aged 6-14. Sports include soccer, basketball, and volleyball. Register at the rec center.',
         'EVENT', 15, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
        ('Neighborhood History Walk',
         'Join local historian Dr. Carter for a guided walk through the neighborhood''s history this Sunday at 10 AM. Meeting point at the old town clock. Free event.',
         'EVENT', 3, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

    -- DISCUSSION posts (15 posts ~25%)
    INSERT INTO posts (title, body, category, author_id, created_at, updated_at) VALUES
        ('Should We Add More Speed Bumps on Oak Street?',
         'I have noticed cars speeding through Oak Street especially in the mornings. What do other residents think about adding more speed bumps? Let''s discuss.',
         'DISCUSSION', 5, NOW() - INTERVAL '86 days', NOW() - INTERVAL '86 days'),
        ('Ideas for the Empty Lot on 3rd Avenue',
         'The empty lot on 3rd has been vacant for 2 years now. I''d love to hear community ideas. A pocket park? A community garden? A basketball court?',
         'DISCUSSION', 11, NOW() - INTERVAL '81 days', NOW() - INTERVAL '81 days'),
        ('Parking Issues Near the School',
         'The congestion during school drop off is getting dangerous. Parents double parking on Birch Street is a real hazard. What can we do as a community?',
         'DISCUSSION', 7, NOW() - INTERVAL '76 days', NOW() - INTERVAL '76 days'),
        ('Thoughts on the New Playground Equipment',
         'Now that the park renovation is done I wanted to hear what parents and kids think about the new equipment. My kids love the climbing wall but miss the old swings.',
         'DISCUSSION', 2, NOW() - INTERVAL '72 days', NOW() - INTERVAL '72 days'),
        ('Local Business Support - How Can We Help?',
         'Several small businesses on Main Street are struggling since the road closures. What are practical ways we can support them during this period?',
         'DISCUSSION', 16, NOW() - INTERVAL '66 days', NOW() - INTERVAL '66 days'),
        ('Community Garden Waitlist - Is It Fair?',
         'The community garden waitlist is 2 years long. Should we prioritize longtime residents? Families with children? Or strictly first come first served?',
         'DISCUSSION', 8, NOW() - INTERVAL '61 days', NOW() - INTERVAL '61 days'),
        ('Street Noise from the Bar on 5th - Solutions?',
         'The new bar on 5th Ave is generating noise past midnight on weekends. Has anyone spoken to the owner? Should we file a formal complaint with the city?',
         'DISCUSSION', 4, NOW() - INTERVAL '56 days', NOW() - INTERVAL '56 days'),
        ('What Should the New Community Center Prioritize?',
         'With the community center opening soon, what programs should they prioritize? Youth after-school programs? Senior services? Fitness classes? Share your thoughts.',
         'DISCUSSION', 13, NOW() - INTERVAL '51 days', NOW() - INTERVAL '51 days'),
        ('Stray Cat Population - Humane Solutions',
         'The stray cat population has grown noticeably. I would like to organize a TNR (Trap-Neuter-Return) program. Who would be willing to help or contribute?',
         'DISCUSSION', 5, NOW() - INTERVAL '44 days', NOW() - INTERVAL '44 days'),
        ('Are We Happy with the New Bus Route?',
         'The new bus route 47 has been running for a month now. Has it improved your commute? Are the timings convenient? Let''s give feedback to the transit authority.',
         'DISCUSSION', 17, NOW() - INTERVAL '37 days', NOW() - INTERVAL '37 days'),
        ('Proposal: Monthly Neighborhood Newsletter',
         'I think we need a monthly printed newsletter for residents who are not online. I can help organize it if others are willing to contribute articles and distribute.',
         'DISCUSSION', 9, NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days'),
        ('Composting Program Interest Check',
         'Would residents be interested in a shared composting program? I have researched the costs and it would be about $5 per household per month. Thoughts?',
         'DISCUSSION', 1, NOW() - INTERVAL '22 days', NOW() - INTERVAL '22 days'),
        ('Sidewalk Condition on Maple Street',
         'The sidewalks on Maple Street between 2nd and 4th are in terrible condition with cracked slabs that are a trip hazard. Has anyone reported this to the city?',
         'DISCUSSION', 6, NOW() - INTERVAL '16 days', NOW() - INTERVAL '16 days'),
        ('Feedback on Community Board App',
         'Now that we have this community board app what features would you like to see added? I personally would love push notifications for ALERT posts.',
         'DISCUSSION', 18, NOW() - INTERVAL '8 days', NOW() - INTERVAL '8 days'),
        ('Best Local Restaurants - Recommendations',
         'Let''s support local! Share your favorite restaurants and hidden gems in our neighborhood. I''ll start: The Corner Diner on 2nd Ave has amazing breakfast.',
         'DISCUSSION', 3, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days');

    -- ALERT posts (9 posts ~15%)
    INSERT INTO posts (title, body, category, author_id, created_at, updated_at) VALUES
        ('URGENT: Water Main Break on Cedar Street',
         'There is a water main break on Cedar Street between 4th and 6th Ave. Residents in this area may experience low water pressure. City crews are on site.',
         'ALERT', 19, NOW() - INTERVAL '84 days', NOW() - INTERVAL '84 days'),
        ('Power Outage Scheduled - Sunday 6 AM to 2 PM',
         'The electricity company has scheduled maintenance that will cause a power outage for the eastern blocks this Sunday from 6 AM to 2 PM. Plan accordingly.',
         'ALERT', 20, NOW() - INTERVAL '79 days', NOW() - INTERVAL '79 days'),
        ('Suspicious Vehicle Reported - Stay Alert',
         'A resident reported a suspicious dark SUV circling the block on Elm and 3rd multiple times last evening. If you see anything unusual please call the non-emergency police line.',
         'ALERT', 1, NOW() - INTERVAL '74 days', NOW() - INTERVAL '74 days'),
        ('Gas Leak Reported Near Park - Avoid Area',
         'A gas smell has been reported near the north entrance of Riverside Park. Please avoid the area. The gas company has been notified and crews are en route.',
         'ALERT', 7, NOW() - INTERVAL '62 days', NOW() - INTERVAL '62 days'),
        ('Missing Dog - Please Help Find Buddy',
         'Our golden retriever Buddy went missing yesterday afternoon near Oak Street Park. He is 3 years old, wearing a red collar with tags. Please call 555-0142 if found.',
         'ALERT', 14, NOW() - INTERVAL '47 days', NOW() - INTERVAL '47 days'),
        ('Flash Flood Warning - Basement Precautions',
         'A flash flood warning has been issued for our area for the next 48 hours. Residents with basements should move valuables to higher ground and check drainage.',
         'ALERT', 19, NOW() - INTERVAL '32 days', NOW() - INTERVAL '32 days'),
        ('Break-In Reported on Maple Street - Lock Up',
         'Police have confirmed a break-in at a residence on Maple Street last night. Residents are advised to ensure all doors and windows are locked and report anything suspicious.',
         'ALERT', 3, NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
        ('Road Closure: Bridge Inspection This Weekend',
         'The Oak Street bridge will be closed this Saturday and Sunday for a structural inspection. All traffic must use the Elm Street diversion. Allow extra travel time.',
         'ALERT', 20, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
        ('Heat Advisory: Check on Elderly Neighbors',
         'A heat advisory is in effect for the next 3 days with temperatures expected to reach 38°C. Please check on elderly or vulnerable neighbors. Cooling centers are open.',
         'ALERT', 6, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');


    -- ---------------------------------------------------------
    -- SEED COMMENTS (200+ comments)
    -- Post IDs are sequential from inserts above:
    --   Posts  1-18: NEWS
    --   Posts 19-36: EVENT
    --   Posts 37-51: DISCUSSION
    --   Posts 52-60: ALERT
    -- ---------------------------------------------------------
    INSERT INTO comments (content, post_id, author_id, created_at) VALUES
    -- Post 1 (Community Center - NEWS)
    ('This is such great news! I have been waiting for this for years.', 1, 2, NOW() - INTERVAL '87 days'),
    ('Will there be parking available at the community center?', 1, 3, NOW() - INTERVAL '87 days'),
    ('I heard they will have a senior fitness program. My parents will love this!', 1, 4, NOW() - INTERVAL '86 days'),
    ('Does anyone know the opening hours? I work until 6 PM most days.', 1, 5, NOW() - INTERVAL '86 days'),
    -- Post 2 (Road Construction - NEWS)
    ('Thanks for the heads up! I will adjust my route.', 2, 1, NOW() - INTERVAL '84 days'),
    ('How long exactly will this take? I have deliveries scheduled.', 2, 6, NOW() - INTERVAL '84 days'),
    ('This road has needed work for ages. Happy they are finally fixing it.', 2, 7, NOW() - INTERVAL '83 days'),
    -- Post 3 (School Award - NEWS)
    ('Congratulations to Principal Adams and all the teachers! Well deserved.', 3, 8, NOW() - INTERVAL '79 days'),
    ('My children go to Jefferson. We are so proud of this recognition!', 3, 9, NOW() - INTERVAL '79 days'),
    ('Three years in a row is outstanding. What is their secret?', 3, 10, NOW() - INTERVAL '78 days'),
    ('Great teachers make great schools. Kudos to the entire staff.', 3, 11, NOW() - INTERVAL '78 days'),
    -- Post 4 (Bus Route - NEWS)
    ('Finally! The old route made my commute 45 minutes longer than necessary.', 4, 12, NOW() - INTERVAL '74 days'),
    ('Does anyone know if the new route passes by Central Station?', 4, 13, NOW() - INTERVAL '74 days'),
    ('Every 20 minutes is great for peak hours. What about off-peak?', 4, 14, NOW() - INTERVAL '73 days'),
    -- Post 5 (Community Garden - NEWS)
    ('I have been on the waitlist for 18 months. This is amazing news!', 5, 15, NOW() - INTERVAL '69 days'),
    ('Can families share a plot or is it one per household?', 5, 16, NOW() - INTERVAL '69 days'),
    ('Will there be a water connection available for the new plots?', 5, 17, NOW() - INTERVAL '68 days'),
    ('I hope they include raised beds for accessibility.', 5, 18, NOW() - INTERVAL '68 days'),
    -- Post 6 (Library Hours - NEWS)
    ('The extended hours will be perfect for students and working adults.', 6, 19, NOW() - INTERVAL '64 days'),
    ('What digital lending services will be available? E-books? Audiobooks?', 6, 20, NOW() - INTERVAL '64 days'),
    ('The library has always been the heart of this community.', 6, 1, NOW() - INTERVAL '63 days'),
    -- Post 7 (Street Lighting - NEWS)
    ('About time! Walking home at night has been dangerous on some streets.', 7, 2, NOW() - INTERVAL '59 days'),
    ('Will this include the alley behind Cedar Street? It is very dark there.', 7, 3, NOW() - INTERVAL '59 days'),
    ('LED upgrades will also save the city a lot on electricity bills.', 7, 4, NOW() - INTERVAL '58 days'),
    -- Post 8 (Recycling - NEWS)
    ('The instructions on the bins were a little confusing. Can they add a guide?', 8, 5, NOW() - INTERVAL '54 days'),
    ('I love the organics bin. Composting will really help reduce our footprint.', 8, 6, NOW() - INTERVAL '54 days'),
    ('Where do we put old paint cans? I could not find that information.', 8, 7, NOW() - INTERVAL '53 days'),
    ('Great initiative. I hope residents actually use the bins correctly.', 8, 8, NOW() - INTERVAL '53 days'),
    -- Post 9 (Neighborhood Watch - NEWS)
    ('I signed up as a block coordinator. Happy to help keep our streets safe.', 9, 9, NOW() - INTERVAL '49 days'),
    ('What time is the training on Saturday? I want to attend.', 9, 10, NOW() - INTERVAL '49 days'),
    ('The watch program has already made a difference in my block.', 9, 11, NOW() - INTERVAL '48 days'),
    -- Post 10 (Park Renovation - NEWS)
    ('The new playground is fantastic! Kids are loving it.', 10, 12, NOW() - INTERVAL '44 days'),
    ('The walking paths are so much better now. Great job to whoever planned this.', 10, 13, NOW() - INTERVAL '44 days'),
    ('Are the benches bolted down? I noticed one was already wobbly.', 10, 14, NOW() - INTERVAL '43 days'),
    ('Can we get a water fountain installed near the playground?', 10, 15, NOW() - INTERVAL '43 days'),
    -- Post 11 (Pharmacy - NEWS)
    ('This is such a great initiative. My dad will definitely use this service.', 11, 12, NOW() - INTERVAL '39 days'),
    ('Do they also check cholesterol? My doctor suggested I get it checked.', 11, 13, NOW() - INTERVAL '39 days'),
    -- Post 14 (Crime Report - NEWS)
    ('Great news for our community! The watch program really made a difference.', 14, 14, NOW() - INTERVAL '20 days'),
    ('Let us keep this momentum going. Stay vigilant everyone.', 14, 15, NOW() - INTERVAL '20 days'),
    ('15% reduction is impressive. What categories of crime decreased most?', 14, 16, NOW() - INTERVAL '19 days'),
    -- Post 15 (Solar - NEWS)
    ('Already applied! The 20% saving would really help with bills.', 15, 17, NOW() - INTERVAL '13 days'),
    ('Is this available to renters or only homeowners?', 15, 18, NOW() - INTERVAL '13 days'),
    ('What happens if I move before the contract ends?', 15, 19, NOW() - INTERVAL '12 days'),
    ('I attended the info session. Happy to answer questions from neighbors.', 15, 20, NOW() - INTERVAL '12 days'),
    -- Post 17 (Dog Park - NEWS)
    ('Finally!! Took the dog there yesterday and he absolutely loved it.', 17, 1, NOW() - INTERVAL '4 days'),
    ('The separate sections for large and small dogs is a great design choice.', 17, 2, NOW() - INTERVAL '4 days'),
    ('The water stations are really well placed. Very thoughtful design.', 17, 3, NOW() - INTERVAL '3 days'),
    ('Noticed some dog waste not being picked up. Please be responsible!', 17, 4, NOW() - INTERVAL '3 days'),
    -- Post 19 (BBQ - EVENT)
    ('Count me in! Should we do a sign-up sheet for what to bring?', 19, 16, NOW() - INTERVAL '86 days'),
    ('Can we bring our dogs? Last year they were not allowed and it was disappointing.', 19, 17, NOW() - INTERVAL '86 days'),
    ('I will bring my famous jerk chicken!', 19, 18, NOW() - INTERVAL '85 days'),
    ('What time does it start? And is there parking nearby?', 19, 19, NOW() - INTERVAL '85 days'),
    ('Should we have a bounce house for the kids this year?', 19, 20, NOW() - INTERVAL '84 days'),
    -- Post 20 (Concert - EVENT)
    ('I went to last year''s concert. It was incredible!', 20, 1, NOW() - INTERVAL '81 days'),
    ('Is there a lineup for the full summer series?', 20, 2, NOW() - INTERVAL '81 days'),
    ('Will there be food vendors? Asking for my stomach.', 20, 3, NOW() - INTERVAL '80 days'),
    -- Post 21 (Cleanup Day - EVENT)
    ('I will be there with my whole family. Let''s make our neighborhood beautiful!', 21, 4, NOW() - INTERVAL '77 days'),
    ('Can we focus on the creek area? It has been filling up with plastic waste.', 21, 5, NOW() - INTERVAL '77 days'),
    ('Will there be refreshments for volunteers?', 21, 6, NOW() - INTERVAL '76 days'),
    ('I will bring my neighbor too. She has been wanting to get involved.', 21, 7, NOW() - INTERVAL '76 days'),
    -- Post 25 (Garage Sale - EVENT)
    ('I have so many items to sell! Registering my address today.', 25, 8, NOW() - INTERVAL '67 days'),
    ('Will there be a map available online? Would love to plan my route.', 25, 9, NOW() - INTERVAL '67 days'),
    ('Found a vintage lamp last year at this sale. Amazing event!', 25, 10, NOW() - INTERVAL '66 days'),
    -- Post 27 (Yoga - EVENT)
    ('I have been looking for a free yoga class! See you Sunday morning.', 27, 11, NOW() - INTERVAL '57 days'),
    ('What level is it suitable for? I am a complete beginner.', 27, 12, NOW() - INTERVAL '57 days'),
    ('All levels welcome according to the post. I started last week, it is wonderful.', 27, 13, NOW() - INTERVAL '56 days'),
    -- Post 29 (Town Hall - EVENT)
    ('Will the meeting be recorded for those who cannot attend in person?', 29, 15, NOW() - INTERVAL '47 days'),
    ('What items are on the budget discussion agenda? Any chance of park improvements?', 29, 16, NOW() - INTERVAL '47 days'),
    ('I will be there. These meetings are important for community voice.', 29, 17, NOW() - INTERVAL '46 days'),
    -- Post 30 (5K Run - EVENT)
    ('Registered already! See everyone at the starting line.', 30, 18, NOW() - INTERVAL '42 days'),
    ('Is there a kids category? My 10 year old wants to run too.', 30, 19, NOW() - INTERVAL '42 days'),
    ('Last year we raised over $5000 for the food bank. Let''s beat that record!', 30, 20, NOW() - INTERVAL '41 days'),
    -- Post 35 (Movie Night - EVENT)
    ('Lion King is a classic choice! Bringing the whole family.', 35, 1, NOW() - INTERVAL '14 days'),
    ('What time does it start exactly? Need to plan dinner before we come.', 35, 2, NOW() - INTERVAL '14 days'),
    ('Will there be popcorn for sale? Just asking the important questions.', 35, 3, NOW() - INTERVAL '13 days'),
    ('Starts at dusk which is around 7:30 PM this time of year.', 35, 4, NOW() - INTERVAL '13 days'),
    -- Post 37 (Speed Bumps - DISCUSSION)
    ('Absolutely agree. Cars go way too fast on that stretch in the mornings.', 37, 11, NOW() - INTERVAL '85 days'),
    ('Speed bumps can damage cars though. Maybe a speed camera instead?', 37, 12, NOW() - INTERVAL '85 days'),
    ('I have seen kids nearly get hit twice this month. Something must be done.', 37, 13, NOW() - INTERVAL '84 days'),
    ('I filed a request with the city 3 months ago and heard nothing back.', 37, 14, NOW() - INTERVAL '84 days'),
    ('Roundabouts are more effective than speed bumps per traffic studies.', 37, 15, NOW() - INTERVAL '83 days'),
    -- Post 38 (Empty Lot - DISCUSSION)
    ('A community garden would be my first choice! We need more green space.', 38, 16, NOW() - INTERVAL '80 days'),
    ('Basketball court! The nearest one is 3 km away from here.', 38, 17, NOW() - INTERVAL '80 days'),
    ('What about a small amphitheater for community events?', 38, 18, NOW() - INTERVAL '79 days'),
    ('I spoke to the city and the lot is actually available for community use proposals.', 38, 19, NOW() - INTERVAL '79 days'),
    ('Pocket park with seating and shade trees would benefit everyone.', 38, 20, NOW() - INTERVAL '78 days'),
    ('Could we combine ideas? Garden on one side, seating area on the other?', 38, 1, NOW() - INTERVAL '78 days'),
    -- Post 39 (Parking School - DISCUSSION)
    ('This has been a problem for years. Parents completely ignore the no-stopping signs.', 39, 2, NOW() - INTERVAL '75 days'),
    ('The school needs to send a letter home to parents about this.', 39, 3, NOW() - INTERVAL '75 days'),
    ('I almost got hit backing out of my driveway during school run last week.', 39, 4, NOW() - INTERVAL '74 days'),
    ('A crossing guard and designated drop-off zone could solve most of this.', 39, 5, NOW() - INTERVAL '74 days'),
    -- Post 40 (Playground - DISCUSSION)
    ('The climbing wall is brilliant! My 8 year old cannot get enough of it.', 40, 6, NOW() - INTERVAL '71 days'),
    ('The swings were always the most popular. Why were they removed?', 40, 7, NOW() - INTERVAL '71 days'),
    ('I think swings will be added in phase 2 of the renovation.', 40, 8, NOW() - INTERVAL '70 days'),
    -- Post 43 (Local Business - DISCUSSION)
    ('We should organize a local shopping weekend where residents commit to buying local.', 43, 8, NOW() - INTERVAL '65 days'),
    ('The coffee shop on the corner has been really struggling. Let''s support them.', 43, 9, NOW() - INTERVAL '65 days'),
    ('I started walking to the local bakery instead of the supermarket. Small change big impact.', 43, 10, NOW() - INTERVAL '64 days'),
    ('A social media campaign with the hashtag could raise visibility for local shops.', 43, 11, NOW() - INTERVAL '64 days'),
    ('The hardware store owner told me business is down 40% since the road closed.', 43, 12, NOW() - INTERVAL '63 days'),
    -- Post 44 (Garden Waitlist - DISCUSSION)
    ('First come first served is the fairest system in my opinion.', 44, 13, NOW() - INTERVAL '60 days'),
    ('Maybe priority for households without private gardens? They need it more.', 44, 14, NOW() - INTERVAL '60 days'),
    ('A lottery system with priority points would balance fairness and need.', 44, 15, NOW() - INTERVAL '59 days'),
    -- Post 45 (Bar Noise - DISCUSSION)
    ('I have called 311 twice already. Nothing has changed.', 45, 16, NOW() - INTERVAL '55 days'),
    ('The music is not the problem as much as the crowd outside at closing time.', 45, 17, NOW() - INTERVAL '55 days'),
    ('Has anyone tried speaking directly with the owner? Might be more effective.', 45, 18, NOW() - INTERVAL '54 days'),
    ('A formal petition might carry more weight than individual complaints.', 45, 19, NOW() - INTERVAL '54 days'),
    -- Post 46 (Community Center Programs - DISCUSSION)
    ('Definitely after-school programs. There is nowhere safe for kids to go after 3 PM.', 46, 20, NOW() - INTERVAL '50 days'),
    ('Senior services please! The older residents in this community are underserved.', 46, 1, NOW() - INTERVAL '50 days'),
    ('Mental health resources and support groups would be incredibly valuable.', 46, 2, NOW() - INTERVAL '49 days'),
    ('Job training workshops for young adults transitioning out of school.', 46, 3, NOW() - INTERVAL '49 days'),
    ('All of the above! The center should serve every age group.', 46, 4, NOW() - INTERVAL '48 days'),
    -- Post 50 (Newsletter - DISCUSSION)
    ('I love this idea! My elderly neighbor does not use the internet at all.', 50, 5, NOW() - INTERVAL '29 days'),
    ('I can help with design and layout. I work in graphic design.', 50, 6, NOW() - INTERVAL '29 days'),
    ('What language should it be in? Our block has many Spanish speaking residents.', 50, 7, NOW() - INTERVAL '28 days'),
    ('Bilingual newsletter would serve our diverse community best.', 50, 8, NOW() - INTERVAL '28 days'),
    -- Post 51 (Composting - DISCUSSION)
    ('$5 per month is very reasonable for a shared composting program.', 51, 9, NOW() - INTERVAL '21 days'),
    ('How would the collection work? Weekly pickup or a shared drop point?', 51, 10, NOW() - INTERVAL '21 days'),
    ('I already compost at home but a shared program would help those without space.', 51, 11, NOW() - INTERVAL '20 days'),
    -- Post 52 (Water Main - ALERT)
    ('I just checked and my water pressure is very low. Hope it is fixed soon.', 52, 9, NOW() - INTERVAL '83 days'),
    ('I live on Cedar St. There are about 5 city trucks outside right now.', 52, 10, NOW() - INTERVAL '83 days'),
    ('How long will this take to fix? I need to shower before work.', 52, 11, NOW() - INTERVAL '83 days'),
    ('Do we need to boil water as a precaution?', 52, 12, NOW() - INTERVAL '82 days'),
    ('City just updated their website saying repairs will take 4-6 hours.', 52, 13, NOW() - INTERVAL '82 days'),
    -- Post 53 (Power Outage - ALERT)
    ('Thanks for the early notice. Will stock up on candles and charge my devices.', 53, 14, NOW() - INTERVAL '78 days'),
    ('Is the whole neighborhood affected or just eastern blocks?', 53, 15, NOW() - INTERVAL '78 days'),
    ('What about people who depend on medical equipment? Is there any support?', 53, 16, NOW() - INTERVAL '77 days'),
    ('8 hours is a long time. I hope they finish ahead of schedule.', 53, 17, NOW() - INTERVAL '77 days'),
    -- Post 54 (Suspicious Vehicle - ALERT)
    ('I saw the same vehicle! It was parked outside our house for about an hour.', 54, 18, NOW() - INTERVAL '73 days'),
    ('I have noted the plate number. Should I share it here or call it in directly?', 54, 19, NOW() - INTERVAL '73 days'),
    ('Please call the non-emergency line rather than posting plates online.', 54, 20, NOW() - INTERVAL '72 days'),
    ('Our ring camera caught the vehicle. I am sharing footage with police.', 54, 1, NOW() - INTERVAL '72 days'),
    -- Post 55 (Gas Leak - ALERT)
    ('I can smell it from my house on the corner. Keeping windows closed.', 55, 2, NOW() - INTERVAL '61 days'),
    ('My kids play at that park entrance every day after school. So glad for the warning.', 55, 3, NOW() - INTERVAL '61 days'),
    ('Update: The gas company truck arrived. They are digging near the north gate.', 55, 4, NOW() - INTERVAL '60 days'),
    ('All clear was given at 4 PM. Area is safe to use again.', 55, 5, NOW() - INTERVAL '60 days'),
    -- Post 56 (Missing Dog - ALERT)
    ('Shared this on our block WhatsApp group. Hope Buddy comes home soon.', 56, 6, NOW() - INTERVAL '46 days'),
    ('I think I saw a golden retriever near the school this morning!', 56, 7, NOW() - INTERVAL '46 days'),
    ('Update: Buddy was found safe by a family on Birch Street! Thank you all!', 56, 14, NOW() - INTERVAL '45 days'),
    -- Post 57 (Flood Warning - ALERT)
    ('Moved everything in my basement to higher shelves. Thanks for the warning.', 57, 5, NOW() - INTERVAL '31 days'),
    ('The storm drain on my street is already blocked. Reported it to the city.', 57, 6, NOW() - INTERVAL '31 days'),
    ('Anyone have sandbags available to share? I only have a few.', 57, 7, NOW() - INTERVAL '30 days'),
    -- Post 58 (Break-In - ALERT)
    ('I have been meaning to get a security camera. This is the push I needed.', 58, 8, NOW() - INTERVAL '17 days'),
    ('Our street WhatsApp group is now on high alert. Thanks for the warning.', 58, 9, NOW() - INTERVAL '17 days'),
    ('Police patrol has been increased in the area according to the precinct.', 58, 10, NOW() - INTERVAL '16 days'),
    -- Post 59 (Bridge Closure - ALERT)
    ('Will the Elm Street diversion handle the extra traffic? It is always congested.', 59, 8, NOW() - INTERVAL '6 days'),
    ('The bridge inspection is long overdue. Better safe than sorry.', 59, 9, NOW() - INTERVAL '6 days'),
    ('Cycling route is also closed I assume? Need to plan an alternate way to work.', 59, 10, NOW() - INTERVAL '5 days'),
    -- Post 60 (Heat Advisory - ALERT)
    ('Already checked on Mrs. Johnson next door. She is doing well and has a fan.', 60, 11, NOW() - INTERVAL '23 hours'),
    ('Where are the cooling centers located exactly?', 60, 12, NOW() - INTERVAL '22 hours'),
    ('The community center and main library are both open as cooling centers.', 60, 13, NOW() - INTERVAL '20 hours'),
    ('Stay hydrated everyone! And look out for your neighbors.', 60, 14, NOW() - INTERVAL '18 hours');

END IF;
END $$;