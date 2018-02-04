% function Result = RUN(INSTANCE, A, B)
% Using INSTANCE as a seed, this function creates a pseudo-random number 
% drawn from a uniform distribution in the open interval (-1,1) and returns
% the value of Result, where Result = A*B + r
function Result = RUN(INSTANCE,A,B)

rng('default');
rng(INSTANCE);

minValue = -1;
maxValue = 1;
r = (maxValue - minValue) * rand(1) + minValue;
Result = A * B + r;
fprintf('Result for irace=%g\n', Result);
end

