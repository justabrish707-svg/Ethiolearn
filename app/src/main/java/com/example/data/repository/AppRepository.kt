package com.example.data.repository

import com.example.data.db.AppDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {

    val grades: Flow<List<Grade>> = appDao.getGrades()

    fun getSubjectsByGrade(gradeId: Int): Flow<List<Subject>> = appDao.getSubjectsByGrade(gradeId)

    fun getUnitsBySubject(subjectId: Int): Flow<List<UnitTable>> = appDao.getUnitsBySubject(subjectId)

    fun getTopicsByUnit(unitId: Int): Flow<List<Topic>> = appDao.getTopicsByUnit(unitId)

    fun searchTopics(query: String): Flow<List<Topic>> = appDao.searchTopics(query)

    suspend fun getTopicById(topicId: Int): Topic? = appDao.getTopicById(topicId)
    
    suspend fun getUnitById(unitId: Int): UnitTable? = appDao.getUnitById(unitId)

    fun getLessonByTopic(topicId: Int): Flow<Lesson?> = appDao.getLessonByTopic(topicId)

    fun getExamplesByTopic(topicId: Int): Flow<List<Example>> = appDao.getExamplesByTopic(topicId)

    fun getPracticeQuestionsByTopic(topicId: Int): Flow<List<PracticeQuestion>> = appDao.getPracticeQuestionsByTopic(topicId)

    fun getQuizQuestionsByTopic(topicId: Int): Flow<List<QuizQuestion>> = appDao.getQuizQuestionsByTopic(topicId)
    
    fun getAllProgress(): Flow<List<Progress>> = appDao.getAllProgress()

    fun getProgressByTopic(topicId: Int): Flow<Progress?> = appDao.getProgressByTopic(topicId)

    suspend fun updateProgress(progress: Progress) {
        appDao.insertProgress(progress)
    }

    suspend fun initializePrepopulatedData() {
        if (appDao.countGrades() == 0) {
            val grades = listOf(
                Grade(9, "Grade 9"),
                Grade(10, "Grade 10")
            )
            appDao.insertGrades(grades)

            val subjects = listOf(
                Subject(1, 9, "Mathematics"),
                Subject(2, 10, "Mathematics")
            )
            appDao.insertSubjects(subjects)

            val units = listOf(
                // Grade 9 Mathematics (Subject 1)
                UnitTable(1, 1, 1, "The Number System"),
                UnitTable(2, 1, 2, "Equations and Inequalities"),
                UnitTable(3, 1, 3, "Further on Inequalities"),
                UnitTable(4, 1, 4, "Further on Relations and Functions"),
                UnitTable(5, 1, 5, "Trigonometry"),
                UnitTable(6, 1, 6, "Vectors in Two Dimensions"),
                UnitTable(7, 1, 7, "Statistics and Probability"),
                UnitTable(8, 1, 8, "Mathematical Reasoning"),

                // Grade 10 Mathematics (Subject 2)
                UnitTable(9, 2, 1, "Relations and Functions"),
                UnitTable(10, 2, 2, "Polynomial Functions"),
                UnitTable(11, 2, 3, "Exponential and Logarithmic Functions"),
                UnitTable(12, 2, 4, "Trigonometric Functions"),
                UnitTable(13, 2, 5, "Circles"),
                UnitTable(14, 2, 6, "Solid Geometry"),
                UnitTable(15, 2, 7, "Coordinate Geometry"),
                UnitTable(16, 2, 8, "Mathematical Reasoning and Proofs"),
                UnitTable(17, 2, 9, "Statistics and Probability")
            )
            appDao.insertUnits(units)

            val topics = listOf(
                // Grade 9 Topics (Unit 1 to 8)
                // Unit 1: The Number System (ID: 1)
                Topic(1, 1, "1.1", "Introduction to Rational Numbers"),
                Topic(2, 1, "1.1", "Properties of Rational Numbers"),
                Topic(3, 1, "1.1", "Decimal representation of Rational Numbers"),
                Topic(4, 1, "1.2", "Introduction to Irrational Numbers"),
                Topic(5, 1, "1.2", "Decimal representation of Irrational Numbers"),
                Topic(6, 1, "1.2", "Properties of Irrational Numbers"),
                Topic(7, 1, "1.3", "The Real Number Line"),
                Topic(8, 1, "1.3", "Order properties of Real Numbers"),
                Topic(9, 1, "1.3", "Density of Real Numbers"),
                Topic(10, 1, "1.4", "Upper bounds and Lower bounds"),
                Topic(11, 1, "1.4", "Bounded sets"),
                Topic(12, 1, "1.4", "Least upper bounds (Supremum)"),
                Topic(13, 1, "1.4", "Greatest lower bounds (Infimum)"),
                Topic(14, 1, "1.5", "Introduction to Radicals"),
                Topic(15, 1, "1.5", "Simplification of Radicals"),
                Topic(16, 1, "1.5", "Addition and Subtraction of Radicals"),
                Topic(17, 1, "1.5", "Multiplication and Division of Radicals"),
                Topic(18, 1, "1.5", "Rationalizing denominators"),

                // Unit 2: Equations and Inequalities (ID: 2)
                Topic(19, 2, "2.1", "Linear Equations in One Variable"),
                Topic(20, 2, "2.1", "Solving Multi-step Linear Equations"),
                Topic(21, 2, "2.1", "Linear Equation Word Problems"),
                Topic(22, 2, "2.2", "Introduction to Rational Equations"),
                Topic(23, 2, "2.2", "Simplifying Rational Expressions"),
                Topic(24, 2, "2.2", "Solving Rational Equations"),
                Topic(25, 2, "2.3", "Introduction to Absolute Value"),
                Topic(26, 2, "2.3", "Properties of Absolute Value"),
                Topic(27, 2, "2.3", "Solving Absolute Value Equations"),
                Topic(28, 2, "2.4", "Systems of Linear Equations in Two Variables"),
                Topic(29, 2, "2.4", "Solving Systems by Graphing"),
                Topic(30, 2, "2.4", "Solving Systems by Substitution"),
                Topic(31, 2, "2.4", "Solving Systems by Elimination"),
                Topic(32, 2, "2.4", "Applications of Linear Systems"),

                // Unit 3: Further on Inequalities (ID: 3)
                Topic(33, 3, "3.1", "Linear Inequalities in One Variable"),
                Topic(34, 3, "3.1", "Properties of Inequalities"),
                Topic(35, 3, "3.1", "Solving Multi-step Inequalities"),
                Topic(36, 3, "3.2", "Absolute Value Inequalities"),
                Topic(37, 3, "3.2", "Solving Less-Than Absolute Value Inequalities"),
                Topic(38, 3, "3.2", "Solving Greater-Than Absolute Value Inequalities"),
                Topic(39, 3, "3.3", "Systems of Linear Inequalities in Two Variables"),
                Topic(40, 3, "3.3", "Graphing Systems of Inequalities"),

                // Unit 4: Further on Relations and Functions (ID: 4)
                Topic(41, 4, "4.1", "Ordered Pairs"),
                Topic(42, 4, "4.1", "Cartesian Products of Sets"),
                Topic(43, 4, "4.2", "Introduction to Relations"),
                Topic(44, 4, "4.2", "Domain and Range of Relations"),
                Topic(45, 4, "4.2", "Graphing Relations"),
                Topic(46, 4, "4.2", "Inverse of a Relation"),
                Topic(47, 4, "4.3", "Introduction to Functions"),
                Topic(48, 4, "4.3", "Function Notation"),
                Topic(49, 4, "4.3", "The Vertical Line Test"),
                Topic(50, 4, "4.3", "One-to-One Functions"),
                Topic(51, 4, "4.3", "Onto Functions"),
                Topic(52, 4, "4.3", "One-to-One Correspondence"),

                // Unit 5: Trigonometry (ID: 5)
                Topic(53, 5, "5.1", "Angles in Standard Position"),
                Topic(54, 5, "5.1", "Introduction to Trigonometric Ratios"),
                Topic(55, 5, "5.1", "Trig Ratios of Special Angles (30, 45, 60)"),
                Topic(56, 5, "5.1", "Trig Ratios of Trigonometric Quadrantal Angles"),
                Topic(57, 5, "5.2", "Solving Right Triangles"),
                Topic(58, 5, "5.2", "Angles of Elevation"),
                Topic(59, 5, "5.2", "Angles of Depression"),
                Topic(60, 5, "5.2", "Trigonometric Applications"),

                // Unit 6: Vectors in Two Dimensions (ID: 6)
                Topic(61, 6, "6.1", "Scalars and Vectors"),
                Topic(62, 6, "6.1", "Geometric Representation of Vectors"),
                Topic(63, 6, "6.1", "Position Vectors"),
                Topic(64, 6, "6.2", "Vector Addition (Geometric)"),
                Topic(65, 6, "6.2", "Vector Subtraction (Geometric)"),
                Topic(66, 6, "6.2", "Scalar Multiplication"),
                Topic(67, 6, "6.2", "Components of a Vector"),
                Topic(68, 6, "6.2", "Vector Arithmetic in Algebra"),
                Topic(69, 6, "6.2", "Magnitude of a Vector"),

                // Unit 7: Statistics and Probability (ID: 7)
                Topic(70, 7, "7.1", "Introduction to Grouped Data"),
                Topic(71, 7, "7.1", "Frequency Distributions"),
                Topic(72, 7, "7.1", "Class Intervals and Midpoints"),
                Topic(73, 7, "7.1", "Cumulative Frequency Tables"),
                Topic(74, 7, "7.2", "Mean of Grouped Data"),
                Topic(75, 7, "7.2", "Median of Grouped Data"),
                Topic(76, 7, "7.2", "Mode of Grouped Data"),
                Topic(77, 7, "7.3", "Introduction to Probability"),
                Topic(78, 7, "7.3", "Sample Spaces and Events"),
                Topic(79, 7, "7.3", "Addition Rule of Probability"),
                Topic(80, 7, "7.3", "Independent Events"),

                // Unit 8: Mathematical Reasoning (ID: 8)
                Topic(81, 8, "8.1", "Logic Statements and Propositions"),
                Topic(82, 8, "8.1", "Logical Connectives - Negation"),
                Topic(83, 8, "8.1", "Logical Connectives - Conjunction"),
                Topic(84, 8, "8.1", "Logical Connectives - Disjunction"),
                Topic(85, 8, "8.1", "Conditional and Biconditional Statements"),
                Topic(86, 8, "8.1", "Truth Tables construction"),
                Topic(87, 8, "8.1", "Tautologies and Contradictions"),
                Topic(88, 8, "8.1", "Logical Equivalence"),
                Topic(89, 8, "8.2", "Valid and Invalid Arguments"),
                Topic(90, 8, "8.2", "Truth Table Proofs of Validity"),

                // Grade 10 Topics (Unit 9 to 17)
                // Unit 1: Relations and Functions (ID: 9)
                Topic(91, 9, "1.1", "Domain and Range of Inverse Relations"),
                Topic(92, 9, "1.1", "Inverse Functions"),
                Topic(93, 9, "1.2", "Introduction to Composite Functions"),
                Topic(94, 9, "1.2", "Evaluating Composite Functions"),
                Topic(95, 9, "1.2", "Properties of Composites"),

                // Unit 2: Polynomial Functions (ID: 10)
                Topic(96, 10, "2.1", "Introduction to Polynomial Expressions"),
                Topic(97, 10, "2.1", "Degree and Coefficients"),
                Topic(98, 10, "2.2", "Addition and Subtraction of Polynomials"),
                Topic(99, 10, "2.2", "Multiplication of Polynomials"),
                Topic(100, 10, "2.2", "Polynomial Division (Long Division)"),
                Topic(101, 10, "2.2", "Synthetic Division"),
                Topic(102, 10, "2.3", "The Remainder Theorem"),
                Topic(103, 10, "2.3", "The Factor Theorem"),
                Topic(104, 10, "2.3", "Rational Root Theorem"),
                Topic(105, 10, "2.3", "Zeros of Polynomials"),
                Topic(106, 10, "2.3", "Factorization of Polynomials"),
                Topic(107, 10, "2.4", "Symmetry of Polynomial Graphs"),
                Topic(108, 10, "2.4", "End Behavior of Graphs"),
                Topic(109, 10, "2.4", "Turning Points"),
                Topic(110, 10, "2.4", "Graphing Polynomial Functions"),

                // Unit 3: Exponential and Logarithmic Functions (ID: 11)
                Topic(111, 11, "3.1", "Definition of Exponential Functions"),
                Topic(112, 11, "3.1", "Properties of Exponents"),
                Topic(113, 11, "3.1", "Graphing Exponential Functions"),
                Topic(114, 11, "3.1", "Solving Exponential Equations"),
                Topic(115, 11, "3.2", "Definition of Logarithmic Functions"),
                Topic(116, 11, "3.2", "Laws of Logarithms"),
                Topic(117, 11, "3.2", "Common and Natural Logarithms"),
                Topic(118, 11, "3.2", "Graphing Logarithmic Functions"),
                Topic(119, 11, "3.2", "Solving Logarithmic Equations"),
                Topic(120, 11, "3.3", "Applications - Growth and Decay"),
                Topic(121, 11, "3.3", "Applications - Compound Interest"),

                // Unit 4: Trigonometric Functions (ID: 12)
                Topic(122, 12, "4.1", "Radian Measure"),
                Topic(123, 12, "4.1", "Degree and Radian Conversions"),
                Topic(124, 12, "4.1", "Arc Length and Sector Area"),
                Topic(125, 12, "4.2", "Sine, Cosine, Tangent Functions (Unit Circle)"),
                Topic(126, 12, "4.2", "Secant, Cosecant, Cotangent Functions"),
                Topic(127, 12, "4.2", "Trig Functions of Any Angle"),
                Topic(128, 12, "4.2", "Graphs of Sine and Cosine"),
                Topic(129, 12, "4.2", "Graphs of Tangent Function"),
                Topic(130, 12, "4.2", "Amplitude, Period, Phase Shift"),
                Topic(131, 12, "4.3", "Pythagorean Identities"),
                Topic(132, 12, "4.3", "Sum and Difference Formulas"),
                Topic(133, 12, "4.3", "Double Angle Formulas"),

                // Unit 5: Circles (ID: 13)
                Topic(134, 13, "5.1", "Definitions of Circle Elements"),
                Topic(135, 13, "5.1", "Chords and Chord Properties"),
                Topic(136, 13, "5.1", "Tangents of a Circle"),
                Topic(137, 13, "5.1", "Secant Segments"),
                Topic(138, 13, "5.1", "Central Angles and Inscribed Angles"),
                Topic(139, 13, "5.1", "Angles Formed by Secants and Tangents"),
                Topic(140, 13, "5.1", "Circle Geometry Proofs"),

                // Unit 6: Solid Geometry (ID: 14)
                Topic(141, 14, "6.1", "Properties of Prisms"),
                Topic(142, 14, "6.1", "Surface Area of Prisms"),
                Topic(143, 14, "6.1", "Volume of Prisms"),
                Topic(144, 14, "6.1", "Properties of Cylinders"),
                Topic(145, 14, "6.1", "Surface Area of Cylinders"),
                Topic(146, 14, "6.1", "Volume of Cylinders"),
                Topic(147, 14, "6.2", "Properties of Pyramids"),
                Topic(148, 14, "6.2", "Surface Area of Pyramids"),
                Topic(149, 14, "6.2", "Volume of Pyramids"),
                Topic(150, 14, "6.2", "Properties of Cones"),
                Topic(151, 14, "6.2", "Surface Area of Cones"),
                Topic(152, 14, "6.2", "Volume of Cones"),
                Topic(153, 14, "6.3", "Properties of Spheres"),
                Topic(154, 14, "6.3", "Surface Area and Volume of Spheres"),

                // Unit 7: Coordinate Geometry (ID: 15)
                Topic(155, 15, "7.1", "The Distance Formula"),
                Topic(156, 15, "7.1", "The Midpoint Formula"),
                Topic(157, 15, "7.2", "Slope of a Line"),
                Topic(158, 15, "7.2", "Slope-Intercept Form"),
                Topic(159, 15, "7.2", "Point-Slope Form"),
                Topic(160, 15, "7.2", "General Form of Straight Line"),
                Topic(161, 15, "7.2", "Parallel Lines Slopes"),
                Topic(162, 15, "7.2", "Perpendicular Lines Slopes"),
                Topic(163, 15, "7.3", "Standard Equation of Circle"),
                Topic(164, 15, "7.3", "General Equation of Circle"),

                // Unit 8: Mathematical Reasoning and Proofs (ID: 16)
                Topic(165, 16, "8.1", "Logical Arguments"),
                Topic(166, 16, "8.1", "Valid and Invalid Arguments"),
                Topic(167, 16, "8.1", "Direct Proofs"),
                Topic(168, 16, "8.1", "Indirect Proofs / Contradiction"),
                Topic(169, 16, "8.1", "Mathematical Induction"),

                // Unit 9: Statistics and Probability (ID: 17)
                Topic(170, 17, "9.1", "Measures of Dispersion - Grouped Data"),
                Topic(171, 17, "9.1", "Standard Deviation of Grouped Data"),
                Topic(172, 17, "9.1", "Variance of Grouped Data"),
                Topic(173, 17, "9.2", "Introduction to Permutations"),
                Topic(174, 17, "9.2", "Introduction to Combinations"),
                Topic(175, 17, "9.2", "The Binomial Theorem"),
                Topic(176, 17, "9.2", "Bernoulli Trials")
            )
            appDao.insertTopics(topics)

            val lessons = listOf(
                Lesson(1, 1, 
                    summary = "A rational number is any number that can be expressed as the ratio p/q of two integers, where the denominator q is not equal to zero.",
                    key_concepts = "Rational Numbers, Fraction Form (p/q), Non-zero Denominator, Set notation Q.",
                    important_notes = "All integers are rational numbers because any integer n can be written as n/1. Decimals that terminate or repeat are also rational.",
                    formulas = "Q = {p/q : p, q ∈ Z, q ≠ 0}"
                ),
                Lesson(91, 91,
                    summary = "The domain of an inverse relation R⁻¹ is the range of the original relation R. The range of R⁻¹ is the domain of R.",
                    key_concepts = "Domain of Inverse, Range of Inverse, Coordinate Swapping, Ordered Pairs (y, x).",
                    important_notes = "To find the domain and range of an inverse relation, you simply swap the sets of the domain and range of the relation.",
                    formulas = "Dom(R⁻¹) = Ran(R) \n Ran(R⁻¹) = Dom(R)"
                )
            )
            appDao.insertLessons(lessons)

            val examples = listOf(
                Example(1, 1, "Is √2 a rational number?", "Step 1: A rational can be expressed as p/q.\nStep 2: √2 cannot be expressed as a simple fraction.\nStep 3: Therefore, √2 is irrational."),
                Example(2, 91, "If R = {(1,2), (3,4)}, find its inverse relation R⁻¹.", "Step 1: Swapping coordinate order of each ordered pair.\nStep 2: R⁻¹ = {(2,1), (4,3)}.\nStep 3: Thus, domain of R⁻¹ becomes {2, 4} and its range becomes {1, 3}.")
            )
            appDao.insertExamples(examples)

            val practiceQuestions = listOf(
                PracticeQuestion(1, 1, "Easy", "Convert 3/4 into decimals.", "0.75", "Divide 3 by 4, which gives 0.75 as a terminating rational decimal."),
                PracticeQuestion(2, 91, "Medium", "If a relation maps every element of A to exactly one element of B, is its inverse necessarily a function?", "No", "No. For example, if R = {(1,2), (3,2)}, the inverse is {(2,1), (2,3)}, which is not a function as input 2 maps to multiple outputs.")
            )
            appDao.insertPracticeQuestions(practiceQuestions)

            val quizQuestions = listOf(
                // Quiz for Topic 1: Rational Numbers
                QuizQuestion(1, 1, "Which of the following is a rational number?", "[\"\\u221a2\", \"\\u03c0\", \"3/5\", \"\\u221a3\"]", 2),
                QuizQuestion(2, 1, "Is the decimal 0.333... (repeating) rational?", "[\"Yes\", \"No\"]", 0),
                QuizQuestion(3, 1, "Which of the following describes a rational number?", "[\"Can be written as p/q where q is not 0\", \"Must contain radicals\", \"Are never integers\", \"None of the above\"]", 0),
                QuizQuestion(4, 1, "Is 0 a rational number?", "[\"Yes\", \"No\"]", 0),
                QuizQuestion(5, 1, "Can a rational number have a negative denominator?", "[\"Yes\", \"No\"]", 0),
                QuizQuestion(6, 1, "Which statement is true?", "[\"Every integer is a rational number\", \"Every rational number is an integer\", \"Rational numbers are always positive\", \"Irrational numbers are rational\"]", 0),
                QuizQuestion(7, 1, "What is the decimal representation of 1/4?", "[\"0.25\", \"0.4\", \"0.5\", \"0.2\"]", 0),
                QuizQuestion(8, 1, "Is \\u221a16 a rational number?", "[\"Yes, because it equals 4\", \"No, because it is inside a radical\", \"Maybe\", \"None of the above\"]", 0),
                QuizQuestion(9, 1, "What is the additive inverse of 2/3?", "[\"3/2\", \"-2/3\", \"-3/2\", \"1/2\"]", 1),
                QuizQuestion(10, 1, "A terminating decimal is always: ", "[\"Rational\", \"Irrational\", \"An integer\", \"None of the above\"]", 0),

                // Quiz for Topic 91: Inverse Relations
                QuizQuestion(11, 91, "If relation R is {(1,2), (3,4)}, what is the domain of inverse relation R\u207b\u00b9?", "[\"{1, 3}\", \"{2, 4}\", \"{1, 2}\", \"{2, 3}\"]", 1),
                QuizQuestion(12, 91, "If the domain of relation R is {x | x \u2265 0}, what is the range of R\u207b\u00b9?", "[\"{y | y \u2265 0}\", \"{y | y < 0}\", \"All real numbers\", \"Empty set\"]", 0),
                QuizQuestion(13, 91, "To obtain the inverse relation, we swap: ", "[\"Coordinates x and y\", \"Plusses and minuses\", \"Domain elements only\", \"None of the above\"]", 0),
                QuizQuestion(14, 91, "The domain of R is the same as: ", "[\"The domain of R\u207b\u00b9\", \"The range of R\u207b\u00b9\", \"The domain of functions\", \"None of the above\"]", 1),
                QuizQuestion(15, 91, "If R is defined by y = 2x, what is the equation of R\u207b\u00b9?", "[\"y = x/2\", \"y = -2x\", \"y = 2/x\", \"y = x - 2\"]", 0),
                QuizQuestion(16, 91, "If R = {(a,b)}, what is R\u207b\u00b9?", "[\"{(b,a)}\", \"{(a,b)}\", \"{(a,a)}\", \"{(b,b)}\"]", 0),
                QuizQuestion(17, 91, "Inverse of inverse relation (R\u207b\u00b9)\u207b\u00b9 is equal to: ", "[\"R\", \"R\u207b\u00b9\", \"Empty relation\", \"Diagonal relation\"]", 0),
                QuizQuestion(18, 91, "If relation R has domain {1, 2} and range {3, 4}, what is Ran(R\u207b\u00b9)?", "[\"{1, 2}\", \"{3, 4}\", \"{1, 3}\", \"{2, 4}\"]", 0),
                QuizQuestion(19, 91, "Can a non-function relation have an inverse?", "[\"Yes, any relation has an inverse\", \"No, only functions\", \"Only if it is one-to-one\", \"None of the above\"]", 0),
                QuizQuestion(20, 91, "If R is y = x + 3, the inverse relation R\u207b\u00b9 has equation: ", "[\"y = x - 3\", \"y = 3 - x\", \"y = -x - 3\", \"y = x/3\"]", 0)
            )
            appDao.insertQuizQuestions(quizQuestions)
        }
    }
}
