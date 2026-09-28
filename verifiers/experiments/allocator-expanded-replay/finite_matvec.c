/* Bounded finite replay kernel. Keep multiplication rounding, term order and
 * CPython math.fsum; only avoid Python's product-loop overhead and zero terms. */
#define PY_SSIZE_T_CLEAN
#include <Python.h>
#include <math.h>

static PyObject *fsum_callable;

static PyObject *matvec(PyObject *self, PyObject *args) {
    PyObject *matrix, *vector;
    if (!PyArg_ParseTuple(args, "OO", &matrix, &vector)) return NULL;
    if (!PyList_Check(matrix) || !PyList_Check(vector)) {
        PyErr_SetString(PyExc_TypeError, "Finite kernel requires list matrix and vector");
        return NULL;
    }
    Py_ssize_t n = PyList_GET_SIZE(vector), rows = PyList_GET_SIZE(matrix), used = 0;
    Py_ssize_t *indices = PyMem_Malloc((n + 1) * sizeof(Py_ssize_t));
    double *values = PyMem_Malloc((n + 1) * sizeof(double));
    if (!indices || !values) { PyMem_Free(indices); PyMem_Free(values); return PyErr_NoMemory(); }
    for (Py_ssize_t j = 0; j < n; j++) {
        double value = PyFloat_AsDouble(PyList_GET_ITEM(vector, j));
        if (PyErr_Occurred()) goto error;
        if (!isfinite(value)) { PyErr_SetString(PyExc_ValueError, "Non-finite vector"); goto error; }
        if (value != 0.0) { indices[used] = j; values[used++] = value; }
    }
    PyObject *result = PyList_New(rows);
    if (!result) goto error;
    for (Py_ssize_t i = 0; i < rows; i++) {
        PyObject *row = PyList_GET_ITEM(matrix, i);
        if (!PyList_Check(row) || PyList_GET_SIZE(row) != n) {
            PyErr_SetString(PyExc_ValueError, "Matrix dimension mismatch"); Py_DECREF(result); goto error;
        }
        PyObject *products = PyList_New(used);
        if (!products) { Py_DECREF(result); goto error; }
        for (Py_ssize_t k = 0; k < used; k++) {
            double a = PyFloat_AsDouble(PyList_GET_ITEM(row, indices[k]));
            double product = a * values[k];
            if (PyErr_Occurred() || !isfinite(product)) {
                if (!PyErr_Occurred()) PyErr_SetString(PyExc_ValueError, "Non-finite product");
                Py_DECREF(products); Py_DECREF(result); goto error;
            }
            PyObject *value = PyFloat_FromDouble(product);
            if (!value) { Py_DECREF(products); Py_DECREF(result); goto error; }
            PyList_SET_ITEM(products, k, value);
        }
        PyObject *sum = PyObject_CallOneArg(fsum_callable, products);
        Py_DECREF(products);
        if (!sum) { Py_DECREF(result); goto error; }
        PyList_SET_ITEM(result, i, sum);
    }
    PyMem_Free(indices); PyMem_Free(values);
    return result;
error:
    PyMem_Free(indices); PyMem_Free(values);
    return NULL;
}

static PyObject *dot(PyObject *self, PyObject *args) {
    PyObject *a, *b;
    if (!PyArg_ParseTuple(args, "OO", &a, &b)) return NULL;
    PyObject *left = PySequence_Fast(a, "Expected vector");
    PyObject *right = PySequence_Fast(b, "Expected vector");
    if (!left || !right) { Py_XDECREF(left); Py_XDECREF(right); return NULL; }
    Py_ssize_t n = PySequence_Fast_GET_SIZE(left);
    if (n != PySequence_Fast_GET_SIZE(right)) {
        PyErr_SetString(PyExc_ValueError, "Vector dimensions differ");
        Py_DECREF(left); Py_DECREF(right); return NULL;
    }
    PyObject *products = PyList_New(0);
    if (!products) { Py_DECREF(left); Py_DECREF(right); return NULL; }
    for (Py_ssize_t i = 0; i < n; i++) {
        double x = PyFloat_AsDouble(PySequence_Fast_GET_ITEM(left, i));
        double y = PyFloat_AsDouble(PySequence_Fast_GET_ITEM(right, i));
        double p = x * y;
        if (PyErr_Occurred() || !isfinite(p)) {
            if (!PyErr_Occurred()) PyErr_SetString(PyExc_ValueError, "Non-finite product");
            Py_DECREF(products); Py_DECREF(left); Py_DECREF(right); return NULL;
        }
        if (p != 0.0) {
            PyObject *v = PyFloat_FromDouble(p);
            if (!v || PyList_Append(products, v) < 0) {
                Py_XDECREF(v); Py_DECREF(products); Py_DECREF(left); Py_DECREF(right); return NULL;
            }
            Py_DECREF(v);
        }
    }
    Py_DECREF(left); Py_DECREF(right);
    PyObject *result = PyObject_CallOneArg(fsum_callable, products);
    Py_DECREF(products);
    return result;
}

static PyObject *transpose_matvec(PyObject *self, PyObject *args) {
    PyObject *matrix, *vector;
    if (!PyArg_ParseTuple(args, "OO", &matrix, &vector)) return NULL;
    if (!PyList_Check(matrix) || !PyList_Check(vector)) {
        PyErr_SetString(PyExc_TypeError, "Expected list matrix and vector"); return NULL;
    }
    Py_ssize_t rows = PyList_GET_SIZE(matrix);
    if (rows != PyList_GET_SIZE(vector)) {
        PyErr_SetString(PyExc_ValueError, "Matrix/vector dimensions differ"); return NULL;
    }
    if (rows == 0) return PyList_New(0);
    PyObject *first = PyList_GET_ITEM(matrix, 0);
    if (!PyList_Check(first)) { PyErr_SetString(PyExc_TypeError, "Expected list row"); return NULL; }
    Py_ssize_t columns = PyList_GET_SIZE(first);
    for (Py_ssize_t i = 0; i < rows; i++) {
        PyObject *row = PyList_GET_ITEM(matrix, i);
        if (!PyList_Check(row) || PyList_GET_SIZE(row) != columns) {
            PyErr_SetString(PyExc_ValueError, "Matrix dimensions differ"); return NULL;
        }
    }
    PyObject *result = PyList_New(columns);
    if (!result) return NULL;
    for (Py_ssize_t j = 0; j < columns; j++) {
        PyObject *products = PyList_New(rows);
        if (!products) { Py_DECREF(result); return NULL; }
        for (Py_ssize_t i = 0; i < rows; i++) {
            double x = PyFloat_AsDouble(PyList_GET_ITEM(PyList_GET_ITEM(matrix, i), j));
            double y = PyFloat_AsDouble(PyList_GET_ITEM(vector, i));
            double p = x * y;
            if (PyErr_Occurred() || !isfinite(p)) {
                if (!PyErr_Occurred()) PyErr_SetString(PyExc_ValueError, "Non-finite product");
                Py_DECREF(products); Py_DECREF(result); return NULL;
            }
            PyObject *v = PyFloat_FromDouble(p);
            if (!v) { Py_DECREF(products); Py_DECREF(result); return NULL; }
            PyList_SET_ITEM(products, i, v);
        }
        PyObject *sum = PyObject_CallOneArg(fsum_callable, products);
        Py_DECREF(products);
        if (!sum) { Py_DECREF(result); return NULL; }
        PyList_SET_ITEM(result, j, sum);
    }
    return result;
}

static PyMethodDef methods[] = {
    {"matvec", matvec, METH_VARARGS, "Finite ordered fsum matrix product"},
    {"dot", dot, METH_VARARGS, "Finite ordered fsum vector product"},
    {"transpose_matvec", transpose_matvec, METH_VARARGS, "Finite ordered fsum transposed product"},
    {NULL, NULL, 0, NULL}};
static struct PyModuleDef definition = {PyModuleDef_HEAD_INIT, "finite_matvec", NULL, -1, methods};
PyMODINIT_FUNC PyInit_finite_matvec(void) {
    PyObject *math = PyImport_ImportModule("math");
    if (!math) return NULL;
    fsum_callable = PyObject_GetAttrString(math, "fsum");
    Py_DECREF(math);
    if (!fsum_callable) return NULL;
    return PyModule_Create(&definition);
}
