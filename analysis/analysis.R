graph <- read.csv2(
  file = "./src/main/resources/original_graph.csv",
  header = TRUE,
  sep = ","
)

deg2rad <- function (degree) {
  degree * pi / 180.0
}

graph <- graph[, -1]
graph$start_stop_lat <- deg2rad(as.double(graph$start_stop_lat))
graph$start_stop_lon <- deg2rad(as.double(graph$start_stop_lon))
graph$end_stop_lat <- deg2rad(as.double(graph$end_stop_lat))
graph$end_stop_lon <- deg2rad(as.double(graph$end_stop_lon))

haversine <- function (f1, l1, f2, l2) {
  earth_radius <- 6371.0
  fsin <- sin((f2 - f1) / 2) ^ 2
  lsin <- sin((l2 - l1) / 2) ^ 2 * cos(f1) * cos(f2)
  2 * earth_radius * sqrt(fsin + lsin)
}

graph$distance <- haversine(
  graph$start_stop_lat,
  graph$start_stop_lon,
  graph$end_stop_lat,
  graph$end_stop_lon
)

avg_distance <- mean(graph$distance)
print(avg_distance)