-- SQL Script to Generate Medical History Records for nabeeluddin266@gmail.com
-- Run this in your Neon database (healthlink DB)

-- IMPORTANT: If you get error "column details is of type oid but expression is of type text"
-- Run these ALTER statements FIRST to change column types from oid to TEXT:
-- (Only run if the columns are empty or you're okay with data conversion)

-- Uncomment and run these BEFORE the INSERT statement below:
/*
ALTER TABLE medical_records ALTER COLUMN details TYPE TEXT;
ALTER TABLE medical_records ALTER COLUMN description TYPE TEXT;
*/

-- First, get the patient UUID for the email
WITH patient_info AS (
    SELECT id as patient_uuid
    FROM users
    WHERE email = 'nabeeluddin266@gmail.com'
    AND user_type = 'PATIENT'
    AND deleted_at IS NULL
    LIMIT 1
),
-- Medical records data with dates
records_data AS (
    SELECT * FROM (VALUES
        -- Record 1: Hypertension (6 months ago)
        ('Hypertension', 
         'Diagnosed with high blood pressure during routine checkup. Blood pressure readings consistently above 140/90 mmHg.',
         'Treatment: Prescribed Lisinopril 10mg once daily. Lifestyle modifications including reduced sodium intake, regular exercise, and weight management. Regular monitoring of blood pressure required.\n\nMedications: Lisinopril 10mg daily\nDoctor: Dr. Ahmed Raza\nHospital: Aga Khan University Hospital, Karachi',
         'Patient advised to monitor blood pressure at home twice daily. Follow-up appointment scheduled in 3 months.',
         CURRENT_DATE - INTERVAL '6 months'),
        
        -- Record 2: Type 2 Diabetes (1 year ago)
        ('Type 2 Diabetes',
         'Diagnosed with Type 2 Diabetes Mellitus. Fasting blood glucose levels elevated at 180 mg/dL. HbA1c at 7.8%.',
         'Treatment: Started on Metformin 500mg twice daily with meals. Dietary counseling provided. Regular blood sugar monitoring required.\n\nMedications: Metformin 500mg twice daily\nDoctor: Dr. Sadia Khan\nHospital: Shaukat Khanum Memorial Hospital, Lahore',
         'Patient educated about diabetes management, diet, and exercise. Regular foot care and eye exams recommended.',
         CURRENT_DATE - INTERVAL '1 year'),
        
        -- Record 3: Seasonal Allergies (3 months ago)
        ('Seasonal Allergies',
         'Patient experiencing seasonal allergic rhinitis with symptoms of sneezing, runny nose, and itchy eyes during spring.',
         'Treatment: Prescribed Cetirizine 10mg once daily during allergy season. Advised to avoid allergens and use nasal saline spray.\n\nMedications: Cetirizine 10mg daily\nDoctor: Dr. Ali Javed\nHospital: Jinnah Hospital, Lahore',
         'Symptoms improve with medication. Patient advised to start medication 2 weeks before expected allergy season.',
         CURRENT_DATE - INTERVAL '3 months'),
        
        -- Record 4: Lower Back Pain (4 months ago)
        ('Lower Back Pain',
         'Patient presented with acute lower back pain after lifting heavy object. Pain rated 7/10, localized to lumbar region.',
         'Treatment: Prescribed Ibuprofen 400mg three times daily for 5 days. Physical therapy recommended. Advised rest and proper lifting techniques.\n\nMedications: Ibuprofen 400mg TID for 5 days\nDoctor: Dr. Faraz Ahmed\nHospital: Pakistan Institute of Medical Sciences, Islamabad',
         'Pain resolved after 1 week of treatment. Patient advised to strengthen core muscles to prevent recurrence.',
         CURRENT_DATE - INTERVAL '4 months'),
        
        -- Record 5: Upper Respiratory Infection (2 months ago)
        ('Upper Respiratory Infection',
         'Patient presented with cough, congestion, and mild fever (100.2°F). Symptoms started 3 days prior.',
         'Treatment: Symptomatic treatment with rest, hydration, and over-the-counter cough syrup. Prescribed Amoxicillin 500mg twice daily for 7 days.\n\nMedications: Amoxicillin 500mg BID for 7 days\nDoctor: Dr. Hina Malik\nHospital: Services Hospital, Lahore',
         'Symptoms resolved within 1 week. Patient advised to complete full course of antibiotics.',
         CURRENT_DATE - INTERVAL '2 months'),
        
        -- Record 6: Migraine Headaches (8 months ago)
        ('Migraine Headaches',
         'Patient experiencing recurrent migraine headaches, 2-3 episodes per month. Headaches last 4-6 hours with photophobia and nausea.',
         'Treatment: Prescribed Sumatriptan 50mg as needed for acute attacks. Lifestyle modifications including stress management and regular sleep schedule.\n\nMedications: Sumatriptan 50mg PRN\nDoctor: Dr. Usman Tariq\nHospital: National Hospital, Karachi',
         'Patient keeping headache diary to identify triggers. Frequency reduced to 1 episode per month with treatment.',
         CURRENT_DATE - INTERVAL '8 months'),
        
        -- Record 7: Gastritis (5 months ago)
        ('Gastritis',
         'Patient complaining of epigastric pain, bloating, and nausea. Endoscopy revealed mild gastritis.',
         'Treatment: Prescribed Omeprazole 20mg once daily before breakfast. Dietary modifications including avoiding spicy foods and caffeine.\n\nMedications: Omeprazole 20mg daily\nDoctor: Dr. Noor Fatima\nHospital: Liaquat National Hospital, Karachi',
         'Symptoms improved significantly after 2 weeks. Patient advised to continue medication for 4 weeks total.',
         CURRENT_DATE - INTERVAL '5 months'),
        
        -- Record 8: Anxiety Disorder (10 months ago)
        ('Anxiety Disorder',
         'Patient presenting with symptoms of generalized anxiety including excessive worry, restlessness, and difficulty concentrating.',
         'Treatment: Prescribed Sertraline 50mg once daily. Referred to counseling services. Stress management techniques taught.\n\nMedications: Sertraline 50mg daily\nDoctor: Dr. Mehreen Iqbal\nHospital: Fountain House, Lahore',
         'Patient showing improvement with medication and therapy. Regular follow-ups scheduled.',
         CURRENT_DATE - INTERVAL '10 months'),
        
        -- Record 9: Vitamin D Deficiency (7 months ago)
        ('Vitamin D Deficiency',
         'Routine blood work revealed Vitamin D deficiency with levels at 18 ng/mL (normal range: 30-100 ng/mL).',
         'Treatment: Prescribed Vitamin D3 50,000 IU once weekly for 8 weeks, then maintenance dose of 2000 IU daily.\n\nMedications: Vitamin D3 50,000 IU weekly for 8 weeks, then 2000 IU daily\nDoctor: Dr. Ayesha Rauf\nHospital: Combined Military Hospital, Rawalpindi',
         'Repeat blood work after 8 weeks showed improvement to 32 ng/mL. Patient continuing maintenance dose.',
         CURRENT_DATE - INTERVAL '7 months')
    ) AS t(title, summary, details, description, record_date)
)
-- Insert 9 medical history records with realistic data
INSERT INTO medical_records (
    id,
    patient_id,
    doctor_id,
    record_type,
    title,
    summary,
    details,
    description,
    attachment_url,
    file_url,
    created_at,
    updated_at,
    created_by,
    last_modified_by,
    version,
    deleted_at
)
SELECT
    gen_random_uuid() as id,
    patient_uuid as patient_id,
    NULL as doctor_id,  -- No doctor association for patient-entered records
    'CONSULTATION'::varchar(60) as record_type,
    rd.title::varchar(180),
    rd.summary::varchar(500),
    rd.details::text as details,  -- Cast to text (if column is oid, this may need adjustment)
    rd.description::varchar(2000),
    NULL as attachment_url,
    NULL as file_url,
    rd.record_date as created_at,
    rd.record_date as updated_at,
    NULL as created_by,
    NULL as last_modified_by,
    0::bigint as version,
    NULL as deleted_at
FROM patient_info
CROSS JOIN records_data rd
WHERE patient_uuid IS NOT NULL;

-- Verify the records were created
SELECT 
    mr.id,
    mr.title,
    mr.record_type,
    mr.created_at,
    u.email as patient_email
FROM medical_records mr
JOIN users u ON mr.patient_id = u.id
WHERE u.email = 'nabeeluddin266@gmail.com'
ORDER BY mr.created_at DESC;

