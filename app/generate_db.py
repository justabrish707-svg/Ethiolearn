import json
import sqlite3
import os

# Create or overwrite the database file
db_path = '/app/applet/app/src/main/assets/database/curriculum.db'
os.makedirs(os.path.dirname(db_path), exist_ok=True)
if os.path.exists(db_path):
    os.remove(db_path)

conn = sqlite3.connect(db_path)
c = conn.cursor()

# Create tables
c.execute("CREATE TABLE IF NOT EXISTS `grades` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))")
c.execute("CREATE TABLE IF NOT EXISTS `subjects` (`id` INTEGER NOT NULL, `grade_id` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`grade_id`) REFERENCES `grades`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
c.execute("CREATE TABLE IF NOT EXISTS `units` (`id` INTEGER NOT NULL, `subject_id` INTEGER NOT NULL, `unit_number` INTEGER NOT NULL, `title` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`subject_id`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
c.execute("CREATE TABLE IF NOT EXISTS `topics` (`id` INTEGER NOT NULL, `unit_id` INTEGER NOT NULL, `section_number` TEXT NOT NULL, `title` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`unit_id`) REFERENCES `units`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
c.execute("CREATE TABLE IF NOT EXISTS `lessons` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `topic_id` INTEGER NOT NULL, `content` TEXT NOT NULL, `video_url` TEXT, FOREIGN KEY(`topic_id`) REFERENCES `topics`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
c.execute("CREATE TABLE IF NOT EXISTS `examples` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `topic_id` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `solution` TEXT, FOREIGN KEY(`topic_id`) REFERENCES `topics`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
c.execute("CREATE TABLE IF NOT EXISTS `quiz_questions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `topic_id` INTEGER NOT NULL, `learning_objective_id` INTEGER, `question_text` TEXT NOT NULL, `options_json` TEXT NOT NULL, `correct_option` INTEGER NOT NULL, `explanation` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `question_type` TEXT NOT NULL, FOREIGN KEY(`topic_id`) REFERENCES `topics`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`learning_objective_id`) REFERENCES `learning_objectives`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
c.execute("CREATE TABLE IF NOT EXISTS `progress` (`topic_id` INTEGER NOT NULL, `status` TEXT NOT NULL, `completed_lessons` INTEGER NOT NULL, `quiz_score` INTEGER NOT NULL, `last_position` TEXT, `last_study_date` INTEGER NOT NULL, `mastery_score` INTEGER NOT NULL, PRIMARY KEY(`topic_id`), FOREIGN KEY(`topic_id`) REFERENCES `topics`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
c.execute("CREATE TABLE IF NOT EXISTS `learning_objectives` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `topic_id` INTEGER NOT NULL, `objective_text` TEXT NOT NULL, FOREIGN KEY(`topic_id`) REFERENCES `topics`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
c.execute("CREATE TABLE IF NOT EXISTS `study_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `duration_minutes` INTEGER NOT NULL, `topics_covered` TEXT NOT NULL)")
c.execute("CREATE TABLE IF NOT EXISTS `achievements` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `icon_res` INTEGER NOT NULL, `date_earned` INTEGER, PRIMARY KEY(`id`))")
c.execute("CREATE TABLE IF NOT EXISTS `exams` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `type` TEXT NOT NULL, `grade_id` INTEGER NOT NULL, `subject_id` INTEGER, `duration_minutes` INTEGER NOT NULL, `question_ids` TEXT NOT NULL)")
c.execute("CREATE TABLE IF NOT EXISTS `exam_sessions` (`exam_id` INTEGER NOT NULL, `answers_json` TEXT NOT NULL, `exam_end_timestamp` INTEGER NOT NULL, `current_question_index` INTEGER NOT NULL, `is_finished` INTEGER NOT NULL, `score` INTEGER NOT NULL, PRIMARY KEY(`exam_id`))")
c.execute("CREATE TABLE IF NOT EXISTS `student_profiles` (`id` INTEGER NOT NULL, `current_streak` INTEGER NOT NULL, `longest_streak` INTEGER NOT NULL, `total_study_minutes` INTEGER NOT NULL, `daily_goal_minutes` INTEGER NOT NULL, `weekly_goal_minutes` INTEGER NOT NULL, `last_study_date` INTEGER NOT NULL, PRIMARY KEY(`id`))")

# Insert room master table 
c.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
c.execute("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd72792090bc7416d3ac766a44db49f18')") # Note: we need the right hash

with open('/app/applet/app/curriculum.json', 'r') as f:
    data = json.load(f)

for g in data.get('grades', []):
    grade_id = g['grade_id']
    name = g['name']
    c.execute("INSERT INTO grades (id, name) VALUES (?, ?)", (grade_id, name))
    
    for s in g.get('subjects', []):
        subject_id = s['subject_id']
        s_name = s['name']
        c.execute("INSERT INTO subjects (id, grade_id, name) VALUES (?, ?, ?)", (subject_id, grade_id, s_name))
        
        for u in s.get('units', []):
            unit_id = u['unit_id']
            number = u['number']
            u_name = u['name']
            c.execute("INSERT INTO units (id, subject_id, unit_number, title) VALUES (?, ?, ?, ?)", (unit_id, subject_id, number, u_name))
            
            for sec in u.get('sections', []):
                sec_num = sec['section_number']
                for t in sec.get('topics', []):
                    topic_id = t['topic_id']
                    t_name = t['name']
                    c.execute("INSERT INTO topics (id, unit_id, section_number, title) VALUES (?, ?, ?, ?)", (topic_id, unit_id, sec_num, t_name))

conn.commit()
conn.close()
print("Done")
