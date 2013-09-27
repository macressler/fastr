# extracted from R Benchmark 2.5 (06/2008) [Simon Urbanek]
# http://r.research.att.com/benchmarks/R-benchmark-25.R

# II. Matrix functions
# Cholesky decomposition of a 3000x3000 matrix

# CTK: to ensure repeatability, this is the seed used by libRmath
.Random.seed = c(401L,1234L,5678L)

b25matfunc <- function(args) {
  runs = if (length(args)) as.integer(args[[1]]) else 6L

  res <- 0
  for (i in 1:runs) {
    a <- rnorm(3000*3000)
    dim(a) <- c(3000, 3000)
    a <- crossprod(a, a)
    b <- chol(a)

    # CTK: to ensure materialization, and to get a result to check
    res <- res + sum(b)
  }

  # CTK: to ensure materialization, and to get a result to check
  round( res / runs, digits=5 )
}

# CTK: to make GNU-R print all 5 decimal digits
options(digits=15)

if (!exists("i_am_wrapper")) {
  b25matfunc(commandArgs(trailingOnly=TRUE))
}
