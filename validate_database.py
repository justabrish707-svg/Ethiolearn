#!/usr/bin/env python3
import os
import json
import sqlite3
import sys

def main():
    print("====== EthioLearn SQLite Curriculum Validation Script ======")
    
    # Locate curriculum JSON
    possible_paths = [
        "app/src/main/assets/curriculum.json",
        "app/curriculum.json",
        "curriculum.json"
    ]
    json_path = None
    for p in possible_paths:
        if os.path.exists(p):
            json_path = p
            break
            
    if not json_path:
        print("❌ Error: Could not locate curriculum.json in assets or app directories.")
        sys.exit(1)
        
    print(f"📁 Loaded curriculum source from: {json_path}")
    
    try:
        with open(json_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except Exception as e:
        print(f"❌ Error: Failed to parse curriculum.json: {e}")
        sys.exit(1)
        
    # Setup test sqlite in memory database for direct assertion
    conn = sqlite3.connect(":memory:")
    cursor = conn.cursor()
    
    # Create tables
    cursor.execute("""
    CREATE TABLE grades (
        id INTEGER PRIMARY KEY,
        name TEXT NOT NULL
    );
    """)
    cursor.execute("""
    CREATE TABLE subjects (
        id INTEGER PRIMARY KEY,
        grade_id INTEGER NOT NULL,
        name TEXT NOT NULL,
        FOREIGN KEY(grade_id) REFERENCES grades(id)
    );
    """)
    cursor.execute("""
    CREATE TABLE units (
        id INTEGER PRIMARY KEY,
        subject_id INTEGER NOT NULL,
        unit_number INTEGER NOT NULL,
        title TEXT NOT NULL,
        FOREIGN KEY(subject_id) REFERENCES subjects(id)
    );
    """)
    cursor.execute("""
    CREATE TABLE topics (
        id INTEGER PRIMARY KEY,
        unit_id INTEGER NOT NULL,
        section TEXT NOT NULL,
        title TEXT NOT NULL,
        FOREIGN KEY(unit_id) REFERENCES units(id)
    );
    """)
    
    print("🚀 Seeding in-memory SQLite tables with curriculum details...")
    
    try:
        grades_to_insert = []
        subjects_to_insert = []
        units_to_insert = []
        topics_to_insert = []
        
        for grade in data.get("grades", []):
            grades_to_insert.append((grade["grade_id"], grade["name"]))
            for subj in grade.get("subjects", []):
                subjects_to_insert.append((subj["subject_id"], grade["grade_id"], subj["name"]))
                for unit in subj.get("units", []):
                    units_to_insert.append((unit["unit_id"], subj["subject_id"], unit["number"], unit["name"]))
                    for sec in unit.get("sections", []):
                        section_number = sec.get("section_number", "")
                        for topic in sec.get("topics", []):
                            topics_to_insert.append((topic["topic_id"], unit["unit_id"], section_number, topic["name"]))
                            
        # Perform Bulk Transaction
        cursor.executemany("INSERT INTO grades VALUES (?, ?);", grades_to_insert)
        cursor.executemany("INSERT INTO subjects VALUES (?, ?, ?);", subjects_to_insert)
        cursor.executemany("INSERT INTO units VALUES (?, ?, ?, ?);", units_to_insert)
        cursor.executemany("INSERT INTO topics VALUES (?, ?, ?, ?);", topics_to_insert)
        conn.commit()
        
    except Exception as e:
        print(f"❌ Error during Database Seeding Transaction: {e}")
        conn.close()
        sys.exit(1)
        
    # --- Integrity Auditing ---
    print("\n🧐 Auditing relational integrity and missing dependencies...")
    
    # 1. Assert totals
    cursor.execute("SELECT COUNT(*) FROM topics;")
    actual_topics_count = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM units;")
    actual_units_count = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM subjects;")
    actual_subjects_count = cursor.fetchone()[0]
    
    cursor.execute("SELECT COUNT(*) FROM grades;")
    actual_grades_count = cursor.fetchone()[0]
    
    print(f"📊 Grades: {actual_grades_count}")
    print(f"📚 Subjects: {actual_subjects_count}")
    print(f"📖 Units: {actual_units_count}")
    print(f"📝 Topics Counted: {actual_topics_count}")
    
    # Trace missing foreign key dependency rows (if any)
    missing_dependencies = []
    
    # Check topics with invalid or missing unit_id references
    cursor.execute("""
        SELECT id, title, unit_id FROM topics 
        WHERE unit_id NOT IN (SELECT id FROM units);
    """)
    bad_topics = cursor.fetchall()
    for row in bad_topics:
        missing_dependencies.append(f"Topic {row[0]} ('{row[1]}') references invalid Unit ID {row[2]}")
        
    # Check units with invalid subject_id references
    cursor.execute("""
        SELECT id, title, subject_id FROM units 
        WHERE subject_id NOT IN (SELECT id FROM subjects);
    """)
    bad_units = cursor.fetchall()
    for row in bad_units:
        missing_dependencies.append(f"Unit {row[0]} ('{row[1]}') references invalid Subject ID {row[2]}")
        
    # Check subjects with invalid grade_id references
    cursor.execute("""
        SELECT id, name, grade_id FROM subjects
        WHERE grade_id NOT IN (SELECT id FROM grades);
    """)
    bad_subjects = cursor.fetchall()
    for row in bad_subjects:
        missing_dependencies.append(f"Subject {row[0]} ('{row[1]}') references invalid Grade ID {row[2]}")
        
    # Report Findings
    if missing_dependencies:
        print("⚠️ Warn: Missing dependencies / integrity issues detected!")
        for issue in missing_dependencies:
            print(f"  - {issue}")
    else:
        print("✅ No missing parent-child dependencies or reference leaks detected!")
        
    # Target exact matches
    expected_topics = 176
    if actual_topics_count == expected_topics:
        print(f"\n✅ SUCCESS: Found exactly {actual_topics_count} curriculum topics as expected (100% integrity match)!")
        conn.close()
        sys.exit(0)
    else:
        print(f"\n❌ FAILED MATCH: Found {actual_topics_count} topic rows but expected exactly {expected_topics}.")
        conn.close()
        sys.exit(1)

if __name__ == "__main__":
    main()
