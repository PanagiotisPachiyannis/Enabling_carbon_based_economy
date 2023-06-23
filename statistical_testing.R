# Load necessary packages
install.packages("readr")
library(readr)

# Read data from CSV file
data <- read_csv("/Users/Panos/Desktop/Book1.csv")

# Check the structure of your data
str(data)

# If your data is not in the first column, replace `1` with the appropriate column number
relative_yield_spreads <- data[[1]]
relative_yield_spreads <- as.numeric(relative_yield_spreads)
# Visual inspection of the data using a histogram
hist(relative_yield_spreads, main = "Histogram of Yield Spreads", xlab = "Yield Spread", col = "lightblue", border = "black")

# Visual inspection using a Q-Q plot
qqnorm(relative_yield_spreads, main = "Q-Q Plot of Yield Spreads")
qqline(relative_yield_spreads, col = "red", lwd = 2)

# Shapiro-Wilk test for normality
shapiro_test <- shapiro.test(relative_yield_spreads)
print(shapiro_test)

# Perform wilcoxon signed rank test
wilcox_test <- wilcox.test(relative_yield_spreads, mu = 0, conf.int = TRUE, conf.level = 0.999)

# Print the test results
print(wilcox_test)

